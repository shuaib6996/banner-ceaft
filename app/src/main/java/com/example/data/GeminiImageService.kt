package com.example.data

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

open class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)
class NoImageGeneratedException(message: String) : GeminiException(message)
class InvalidApiKeyException(message: String) : GeminiException(message)
class RateLimitException(message: String) : GeminiException(message)
class GeminiApiException(val statusCode: Int, message: String, val rawBody: String? = null) : GeminiException(message)

class GeminiImageService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sanitizes API key by stripping quotes, newlines, tabs, and spaces.
     */
    fun sanitizeApiKey(key: String): String {
        return key.trim()
            .removeSurrounding("\"", "\"")
            .removeSurrounding("'", "'")
            .replace("\\s+".toRegex(), "")
    }

    /**
     * Sanitizes model identifier by removing redundant 'models/' prefix and trimming.
     */
    fun sanitizeModel(model: String): String {
        return model.trim()
            .removePrefix("models/")
            .trim()
            .ifBlank { AiModelConfig.DEFAULT_MODEL }
    }

    /**
     * Generates an image using either:
     * - Google Imagen 3 (`imagen-3.0-generate-002`, `imagen-3`) via `:predict`
     * - Gemini Image Generation (`gemini-3.1-flash-image`, `gemini-3.1-flash-image-preview`) via `:generateContent`
     * - Gemini Flash/Pro models (`gemini-2.0-flash`, `gemini-2.5-flash`): attempts exact model with graceful
     *   image fallback to Imagen 3 if text model returns HTTP 400 (modality not supported).
     */
    suspend fun generateImage(
        apiKey: String,
        prompt: String,
        aspectRatio: String = "16:9",
        model: String = AiModelConfig.DEFAULT_MODEL
    ): ByteArray = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            throw InvalidApiKeyException("API key is empty. Please enter your Gemini API key in Settings.")
        }

        val cleanModel = sanitizeModel(model)
        val mappedAspectRatio = mapAspectRatio(aspectRatio)

        Log.i(TAG, "Starting image generation for model: '$cleanModel', aspect ratio: '$mappedAspectRatio'")

        // Reject embedding models cleanly as they cannot generate images
        if (cleanModel.contains("embedding", ignoreCase = true)) {
            throw GeminiApiException(
                400,
                "Model '$cleanModel' is a text-embedding model and cannot generate images. Please select an Image or Flash model in Settings."
            )
        }

        // Routing Decision:
        // 1. Imagen models (imagen-3.0-generate-002, imagen-3) -> call :predict
        if (cleanModel.startsWith("imagen", ignoreCase = true)) {
            return@withContext generateWithImagen(cleanKey, cleanModel, prompt, mappedAspectRatio)
        }

        // 2. Gemini dedicated image models (gemini-3.1-flash-image, etc.) -> call :generateContent
        if (cleanModel.contains("image", ignoreCase = true)) {
            return@withContext generateWithGeminiImage(cleanKey, cleanModel, prompt, mappedAspectRatio)
        }

        // 3. Flash / Pro models (gemini-2.0-flash, gemini-2.5-flash, gemini-1.5-flash, etc.):
        // First try the user's selected exact model via generateContent.
        // If Google returns 400 (because standard text Flash does not support IMAGE modality),
        // seamlessly route to Google Imagen 3 so user's banner generation succeeds smoothly.
        try {
            return@withContext generateWithGeminiImage(cleanKey, cleanModel, prompt, mappedAspectRatio)
        } catch (e: GeminiApiException) {
            if (e.statusCode == 400) {
                Log.w(
                    TAG,
                    "Model '$cleanModel' returned HTTP 400 (likely text-only modality). Seamlessly falling back to Google Imagen 3 (imagen-3.0-generate-002)..."
                )
                return@withContext generateWithImagen(cleanKey, "imagen-3.0-generate-002", prompt, mappedAspectRatio)
            } else {
                throw e
            }
        }
    }

    /**
     * Executes Imagen 3 :predict request.
     */
    private suspend fun generateWithImagen(
        apiKey: String,
        modelName: String,
        prompt: String,
        aspectRatio: String
    ): ByteArray {
        val payload = JSONObject().apply {
            put("instances", JSONArray().apply {
                put(JSONObject().apply {
                    put("prompt", prompt)
                })
            })
            put("parameters", JSONObject().apply {
                put("sampleCount", 1)
                put("aspectRatio", aspectRatio)
                put("outputMimeType", "image/png")
            })
        }

        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:predict"
        val (code, body) = executeRequestWithBackoff(endpointUrl, apiKey, payload.toString(), modelName)

        when (code) {
            in 200..299 -> {
                val base64Data = parseImageBase64(body)
                    ?: throw NoImageGeneratedException("Imagen 3 did not return image data. Try adjusting your prompt.")
                val decoded = Base64.decode(base64Data, Base64.DEFAULT)
                ImageSafetyValidator.validateImage(decoded)
                return decoded
            }
            401, 403 -> {
                val err = parseGoogleErrorMessage(body)
                throw InvalidApiKeyException("Invalid or unauthorized API key (HTTP $code): $err. Please check your key in Settings.")
            }
            429 -> {
                val err = parseGoogleErrorMessage(body)
                throw RateLimitException("Google API Quota exceeded (HTTP 429): $err. Please wait a minute before retrying.")
            }
            404 -> {
                // If specific imagen variant not found, fallback once to gemini-3.1-flash-image
                Log.w(TAG, "Imagen model '$modelName' returned 404. Trying gemini-3.1-flash-image...")
                return generateWithGeminiImage(apiKey, "gemini-3.1-flash-image", prompt, aspectRatio)
            }
            else -> {
                val err = parseGoogleErrorMessage(body)
                throw GeminiApiException(code, "Imagen API call failed with status $code: $err", body)
            }
        }
    }

    /**
     * Executes Gemini :generateContent request with IMAGE modality.
     */
    private suspend fun generateWithGeminiImage(
        apiKey: String,
        modelName: String,
        prompt: String,
        aspectRatio: String
    ): ByteArray {
        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("IMAGE")
                })
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", aspectRatio)
                    put("imageSize", "2K")
                })
            })
        }

        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
        val (code, body) = executeRequestWithBackoff(endpointUrl, apiKey, payload.toString(), modelName)

        when (code) {
            in 200..299 -> {
                val base64Data = parseImageBase64(body)
                    ?: throw NoImageGeneratedException("Gemini did not return image data. Try adjusting your prompt.")
                val decoded = Base64.decode(base64Data, Base64.DEFAULT)
                ImageSafetyValidator.validateImage(decoded)
                return decoded
            }
            401, 403 -> {
                val err = parseGoogleErrorMessage(body)
                throw InvalidApiKeyException("Invalid or unauthorized API key (HTTP $code): $err. Please check your key in Settings.")
            }
            429 -> {
                val err = parseGoogleErrorMessage(body)
                throw RateLimitException("Google API Quota exceeded (HTTP 429): $err. Please wait a minute before retrying.")
            }
            404 -> {
                // Try fallback to gemini-3.1-flash-image-preview
                if (modelName != "gemini-3.1-flash-image-preview") {
                    Log.w(TAG, "Model '$modelName' returned 404. Falling back to gemini-3.1-flash-image-preview...")
                    return generateWithGeminiImage(apiKey, "gemini-3.1-flash-image-preview", prompt, aspectRatio)
                }
                val err = parseGoogleErrorMessage(body)
                throw GeminiApiException(404, "Model '$modelName' not found (HTTP 404): $err", body)
            }
            else -> {
                val err = parseGoogleErrorMessage(body)
                throw GeminiApiException(code, "Gemini API call failed with status $code: $err", body)
            }
        }
    }

    /**
     * Edit an existing image with a text prompt using Gemini models.
     */
    suspend fun editImage(
        apiKey: String,
        sourceImageBytes: ByteArray,
        editPrompt: String,
        aspectRatio: String = "16:9",
        model: String = AiModelConfig.DEFAULT_MODEL
    ): ByteArray = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            throw InvalidApiKeyException("API key is empty. Please enter your Gemini API key in Settings.")
        }

        val cleanModel = sanitizeModel(model).let {
            if (it.startsWith("imagen", ignoreCase = true) || it.contains("embedding", ignoreCase = true)) {
                "gemini-3.1-flash-image"
            } else {
                it
            }
        }

        val mappedAspectRatio = mapAspectRatio(aspectRatio)
        val base64Source = Base64.encodeToString(sourceImageBytes, Base64.NO_WRAP)

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/png")
                                put("data", base64Source)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", "Edit this image based on the following instructions: $editPrompt")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("IMAGE")
                })
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", mappedAspectRatio)
                    put("imageSize", "2K")
                })
            })
        }

        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"
        val (code, body) = executeRequestWithBackoff(endpointUrl, cleanKey, payload.toString(), cleanModel)

        when (code) {
            in 200..299 -> {
                val base64Data = parseImageBase64(body)
                    ?: throw NoImageGeneratedException("Couldn't edit image with that prompt. Try adjusting your description.")
                val decoded = Base64.decode(base64Data, Base64.DEFAULT)
                ImageSafetyValidator.validateImage(decoded)
                return@withContext decoded
            }
            401, 403 -> {
                val err = parseGoogleErrorMessage(body)
                throw InvalidApiKeyException("Invalid or unauthorized API key (HTTP $code): $err. Please check your key in Settings.")
            }
            429 -> {
                val err = parseGoogleErrorMessage(body)
                throw RateLimitException("Google API Quota exceeded (HTTP 429): $err. Please wait a minute before retrying.")
            }
            else -> {
                val err = parseGoogleErrorMessage(body)
                throw GeminiApiException(code, "Edit image failed with status $code: $err", body)
            }
        }
    }

    /**
     * Executes HTTP POST call with:
     * - Key passed BOTH as query parameter `?key=...` AND header `x-goog-api-key`
     * - Exponential backoff retry logic on HTTP 429 (Resource Exhausted) and HTTP 503 (Service Unavailable)
     * - Full error logging of HTTP status code and raw response body
     */
    private suspend fun executeRequestWithBackoff(
        endpointUrl: String,
        apiKey: String,
        jsonBody: String,
        modelName: String,
        maxRetries: Int = 3
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val urlWithKey = if (endpointUrl.contains("?")) {
            "$endpointUrl&key=$apiKey"
        } else {
            "$endpointUrl?key=$apiKey"
        }

        var attempt = 0
        var lastCode = 0
        var lastBody = ""

        while (attempt < maxRetries) {
            attempt++
            try {
                // Build a FRESH request on every attempt — OkHttp consumes RequestBody
                // on first execute(), so reusing the same Request object causes retries
                // to send an empty body, leading to silent 400 errors on retry.
                val freshBody = jsonBody.toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(urlWithKey)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(freshBody)
                    .build()

                val response = client.newCall(request).execute()
                response.use { resp ->
                    lastCode = resp.code
                    lastBody = resp.body?.string() ?: ""

                    if (resp.isSuccessful) {
                        Log.i(TAG, "HTTP $lastCode Success for model '$modelName' on attempt $attempt")
                        return@withContext Pair(lastCode, lastBody)
                    }

                    // Log exact status code and raw error response body
                    Log.e(
                        TAG,
                        "API Call Failed! Model: '$modelName', Status: $lastCode, Attempt: $attempt/$maxRetries, URL: $endpointUrl\nRaw Response: ${lastBody.take(500)}"
                    )

                    // If Rate Limited (429) or Server Unavailable (503), apply exponential backoff
                    if ((lastCode == 429 || lastCode == 503) && attempt < maxRetries) {
                        val backoffMs = (1500L * (1 shl (attempt - 1))) + (100L..400L).random()
                        Log.w(
                            TAG,
                            "Transient HTTP $lastCode on attempt $attempt. Backing off ${backoffMs}ms before retry..."
                        )
                        delay(backoffMs)
                        // loop to next attempt
                    } else {
                        // For 400, 401, 403, 404, or after retries exhausted, return immediately
                        return@withContext Pair(lastCode, lastBody)
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "Socket timeout on attempt $attempt/$maxRetries for '$modelName'", e)
                if (attempt < maxRetries) {
                    val backoffMs = 2000L * attempt
                    Log.w(TAG, "Timeout on attempt $attempt. Retrying in ${backoffMs}ms...")
                    delay(backoffMs)
                } else {
                    throw IOException(
                        "The AI model '$modelName' took too long to generate an image for this prompt. " +
                        "Try simplifying or shortening your prompt, or switch to a faster model like 'imagen-3.0-generate-002'."
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network I/O exception on attempt $attempt/$maxRetries for '$modelName'", e)
                if (attempt < maxRetries) {
                    val backoffMs = 1200L * attempt
                    delay(backoffMs)
                } else {
                    throw IOException("Network connection error. Please check your internet connection and try again.")
                }
            }
        }

        Pair(lastCode, lastBody)
    }

    /**
     * Parses Base64 image payload from both Gemini and Imagen response schemas.
     */
    private fun parseImageBase64(jsonResponse: String): String? {
        val root = try {
            JSONObject(jsonResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON response", e)
            return null
        }

        // Safety / Block check on promptFeedback
        val promptFeedback = root.optJSONObject("promptFeedback")
        val blockReason = promptFeedback?.optString("blockReason", "")
        if (!blockReason.isNullOrBlank() && !blockReason.equals("BLOCK_REASON_UNSPECIFIED", ignoreCase = true)) {
            throw ContentModeratedException(
                "⚠️ Content Moderated: Google AI safety filters blocked this prompt ($blockReason). Please modify your topic or description."
            )
        }

        // 1. Gemini generateContent schema: candidates[0].content.parts[0].inlineData.data
        val candidates = root.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val finishReason = candidate.optString("finishReason", "")
            if (finishReason.contains("SAFETY", ignoreCase = true) ||
                finishReason.contains("BLOCK", ignoreCase = true) ||
                finishReason.contains("PROHIBITED", ignoreCase = true)
            ) {
                throw ContentModeratedException(
                    "⚠️ Content Moderated: Google Gemini flagged this prompt under AI safety guidelines ($finishReason). Please adjust your prompt and try again."
                )
            }

            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val data = inlineData.optString("data", "")
                        if (data.isNotBlank()) return data
                    }
                }
            }
        }

        // 2. Google Imagen 3 predict schema: predictions[0].bytesBase64Encoded
        val predictions = root.optJSONArray("predictions")
        if (predictions != null && predictions.length() > 0) {
            val prediction = predictions.getJSONObject(0)
            val safetyAttributes = prediction.optJSONObject("safetyAttributes")
            val blocked = safetyAttributes?.optBoolean("blocked", false) ?: false
            if (blocked) {
                throw ContentModeratedException(
                    "⚠️ Content Moderated: Google Imagen flagged this prompt under AI safety guidelines. Please modify your prompt."
                )
            }

            val b64 = prediction.optString("bytesBase64Encoded", "")
                .ifBlank { prediction.optString("bytesBase64", "") }
                .ifBlank { prediction.optJSONObject("image")?.optString("imageBytes", "") ?: "" }
            if (b64.isNotBlank()) return b64
        }

        return null
    }

    /**
     * Extracts human-readable message from Google Generative Language error JSON.
     */
    private fun parseGoogleErrorMessage(bodyString: String): String {
        return try {
            val root = JSONObject(bodyString)
            val errorObj = root.optJSONObject("error")
            if (errorObj != null) {
                val message = errorObj.optString("message", "")
                val status = errorObj.optString("status", "")
                if (message.isNotBlank()) {
                    if (status.isNotBlank()) "$status: $message" else message
                } else if (status.isNotBlank()) {
                    status
                } else {
                    bodyString.take(250)
                }
            } else {
                bodyString.take(250)
            }
        } catch (e: Exception) {
            bodyString.take(250)
        }
    }

    private fun mapAspectRatio(rawRatio: String): String {
        return when {
            rawRatio.contains("16:9", ignoreCase = true) -> "16:9"
            rawRatio.contains("4:3", ignoreCase = true) -> "4:3"
            rawRatio.contains("1:1", ignoreCase = true) -> "1:1"
            rawRatio.contains("9:16", ignoreCase = true) -> "9:16"
            else -> "16:9"
        }
    }

    companion object {
        private const val TAG = "GeminiImageService"
    }
}

