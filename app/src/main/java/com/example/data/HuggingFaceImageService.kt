package com.example.data

import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Service for Hugging Face Text-to-Image generation using the official
 * Hugging Face Inference Providers infrastructure.
 *
 * Implements serverless provider routing (auto or explicit provider such as fal-ai, together, replicate, wavespeed)
 * and conforms to the official Hugging Face text-to-image API specification.
 */
class HuggingFaceImageService {

    companion object {
        private const val TAG = "HuggingFaceImageService"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun sanitizeApiKey(raw: String): String {
        return raw.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .let {
                if (it.startsWith("Bearer ", ignoreCase = true)) it.substring(7).trim() else it
            }
            .let {
                if (it.startsWith("api key:", ignoreCase = true)) it.substring(8).trim() else it
            }
    }

    /**
     * Resolves the inference provider and endpoint URL for the model.
     *
     * - If provider is explicitly specified (e.g. "fal-ai", "together", "replicate", "wavespeed"),
     *   routes via https://router.huggingface.co/{provider}/models/{model}.
     * - If provider is "auto" (or blank), queries Hugging Face Hub metadata to determine
     *   which partner inference provider currently hosts the model, falling back to direct router.
     */
    suspend fun resolveProviderAndEndpoint(
        model: String,
        apiKey: String,
        provider: String = "auto"
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val cleanModel = model.trim().ifBlank { "black-forest-labs/FLUX.1-schnell" }
        val cleanProvider = provider.trim().lowercase()

        // 1. Explicit provider (non-auto and not a full URL)
        if (cleanProvider.isNotBlank() && cleanProvider != "auto" && !cleanProvider.startsWith("http")) {
            val endpoint = "https://router.huggingface.co/$cleanProvider/models/$cleanModel"
            Log.d(TAG, "Using explicit provider '$cleanProvider': $endpoint")
            return@withContext Pair(cleanProvider, endpoint)
        }

        // 2. Custom full URL if entered in Base URL field
        if (cleanProvider.startsWith("http://") || cleanProvider.startsWith("https://")) {
            val endpoint = if (cleanProvider.endsWith("/")) "$cleanProvider$cleanModel" else "$cleanProvider/$cleanModel"
            Log.d(TAG, "Using custom endpoint: $endpoint")
            return@withContext Pair("custom", endpoint)
        }

        // 3. Auto routing: Query Hugging Face Hub model metadata for live inference providers
        try {
            val hubUrl = "https://huggingface.co/api/models/$cleanModel?expand[]=inferenceProviderMapping"
            val reqBuilder = Request.Builder().url(hubUrl).get()
            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val mapping = json.optJSONObject("inferenceProviderMapping")
                if (mapping != null) {
                    val keys = mapping.keys()
                    while (keys.hasNext()) {
                        val pKey = keys.next()
                        val pObj = mapping.optJSONObject(pKey)
                        val status = pObj?.optString("status")
                        val task = pObj?.optString("task")
                        if ((status == "live" || status == "staging") && (task == null || task == "text-to-image")) {
                            val endpoint = "https://router.huggingface.co/$pKey/models/$cleanModel"
                            Log.d(TAG, "Auto resolved model '$cleanModel' to provider '$pKey': $endpoint")
                            return@withContext Pair(pKey, endpoint)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto provider resolution query failed, using direct router fallback", e)
        }

        // 4. Default fallback: router with model path
        Pair("auto", "https://router.huggingface.co/hf-inference/models/$cleanModel")
    }

    /**
     * Generates an image using Hugging Face Inference Providers.
     *
     * @param apiKey Hugging Face User Access Token (starts with "hf_")
     * @param model Model identifier (e.g. "black-forest-labs/FLUX.1-schnell")
     * @param prompt Text prompt describing the desired image
     * @param provider Provider name (default "auto", or "fal-ai", "together", "replicate", etc.)
     * @param negativePrompt Optional negative prompt
     * @param width Optional width in pixels
     * @param height Optional height in pixels
     * @param numInferenceSteps Optional number of denoising steps
     * @param guidanceScale Optional guidance scale
     * @param onProgress Callback for status updates (e.g. "Model is loading...")
     * @return Raw image bytes (PNG / JPEG)
     */
    suspend fun generateImage(
        apiKey: String,
        model: String,
        prompt: String,
        provider: String = "auto",
        negativePrompt: String? = null,
        width: Int? = null,
        height: Int? = null,
        numInferenceSteps: Int? = null,
        guidanceScale: Double? = null,
        onProgress: ((String) -> Unit)? = null
    ): ByteArray = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            throw IOException("Hugging Face API token is required. Please set it in Settings.")
        }
        val cleanModel = model.trim().ifBlank { "black-forest-labs/FLUX.1-schnell" }

        val (_, endpoint) = resolveProviderAndEndpoint(cleanModel, cleanKey, provider)

        // Build official Hugging Face text-to-image payload
        val payloadJson = JSONObject().apply {
            put("inputs", prompt)
            val params = JSONObject()
            if (!negativePrompt.isNullOrBlank()) {
                params.put("negative_prompt", negativePrompt)
            }
            if (width != null && width > 0) {
                params.put("width", width)
            }
            if (height != null && height > 0) {
                params.put("height", height)
            }
            if (numInferenceSteps != null && numInferenceSteps > 0) {
                params.put("num_inference_steps", numInferenceSteps)
            }
            if (guidanceScale != null && guidanceScale > 0.0) {
                params.put("guidance_scale", guidanceScale)
            }
            if (params.length() > 0) {
                put("parameters", params)
            }
        }

        var attempts = 0
        val maxAttempts = 3

        while (attempts < maxAttempts) {
            attempts++
            val request = Request.Builder()
                .url(endpoint)
                .post(payloadJson.toString().toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Accept", "image/png, image/jpeg, image/*")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Title", "BannerCraft")
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: SocketTimeoutException) {
                throw IOException(
                    "Request timed out. The Hugging Face model '$cleanModel' took too long to generate an image. " +
                        "Please try again or switch to a faster model like 'black-forest-labs/FLUX.1-schnell'.",
                    e
                )
            }

            if (response.isSuccessful) {
                val imageBytes = response.body?.bytes()
                    ?: throw IOException("Empty response received from Hugging Face.")

                // Verify binary data decodes as valid image
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap == null) {
                    val errorSnippet = String(imageBytes, Charsets.UTF_8).take(200)
                    throw IOException("Hugging Face returned an unexpected response format: $errorSnippet")
                }
                bitmap.recycle()

                // Content safety check
                val isSafe = ImageSafetyValidator.isImageSafeAndVisible(imageBytes)
                if (!isSafe) {
                    throw ContentModeratedException(
                        "⚠️ Content Moderated: This prompt was flagged by AI safety guidelines. Please modify your prompt and try again."
                    )
                }

                return@withContext imageBytes
            }

            val statusCode = response.code
            val bodyString = response.body?.string().orEmpty()

            // Handle 503 Model Loading retry with 20s delay
            if (statusCode == 503) {
                if (attempts < maxAttempts) {
                    Log.i(TAG, "Model '$cleanModel' is loading (HTTP 503). Retrying in 20s (attempt $attempts/$maxAttempts)...")
                    onProgress?.invoke("Model is loading...")
                    delay(20000L)
                    continue
                } else {
                    throw IOException("Model is loading... Please wait a moment and try again.")
                }
            }

            // Map standard Hugging Face error codes to friendly messages
            when (statusCode) {
                401 -> throw IOException("Invalid Hugging Face token.")
                403 -> throw IOException("Token lacks Inference Providers permission, or this model needs license acceptance on huggingface.co.")
                404 -> throw IOException("This model is not available on any inference provider. Try another model.")
                402, 429 -> throw IOException("Credits or rate limit exhausted for this key.")
                else -> {
                    val parsedMsg = try {
                        val json = JSONObject(bodyString)
                        val errObj = json.opt("error")
                        when (errObj) {
                            is String -> errObj
                            is org.json.JSONArray -> errObj.optString(0, bodyString)
                            else -> json.optString("message", bodyString)
                        }
                    } catch (_: Exception) {
                        bodyString.take(200)
                    }
                    throw IOException("Hugging Face returned HTTP $statusCode: ${parsedMsg.ifBlank { "Unknown error" }}")
                }
            }
        }

        throw IOException("Failed to generate image from Hugging Face after $maxAttempts attempts.")
    }

    /**
     * Tests connectivity to Hugging Face by validating the user token against the Hub API
     * and checking model accessibility.
     */
    suspend fun testConnection(
        apiKey: String,
        model: String,
        provider: String = "auto"
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext ConnectionTestResult.Failure(401, "Invalid Hugging Face token.")
        }
        val cleanModel = model.trim().ifBlank { "black-forest-labs/FLUX.1-schnell" }
        val startTime = System.currentTimeMillis()

        try {
            // First check: Verify token identity and permissions via /api/whoami-v2
            val whoamiReq = Request.Builder()
                .url("https://huggingface.co/api/whoami-v2")
                .addHeader("Authorization", "Bearer $cleanKey")
                .get()
                .build()

            val whoamiResp = client.newCall(whoamiReq).execute()
            if (whoamiResp.code == 401) {
                return@withContext ConnectionTestResult.Failure(401, "Invalid Hugging Face token.")
            }

            // Second check: Verify model accessibility on Hugging Face Hub
            val modelReq = Request.Builder()
                .url("https://huggingface.co/api/models/$cleanModel")
                .addHeader("Authorization", "Bearer $cleanKey")
                .get()
                .build()

            val modelResp = client.newCall(modelReq).execute()
            val latency = System.currentTimeMillis() - startTime

            if (modelResp.isSuccessful) {
                return@withContext ConnectionTestResult.Success(
                    latencyMs = latency,
                    message = "Connected to Hugging Face! Model '$cleanModel' is verified."
                )
            }

            val humanMsg = when (modelResp.code) {
                401 -> "Invalid Hugging Face token."
                403 -> "Token lacks Inference Providers permission, or this model needs license acceptance on huggingface.co."
                404 -> "This model is not available on any inference provider. Try another model."
                402, 429 -> "Credits or rate limit exhausted for this key."
                else -> "Hugging Face returned HTTP ${modelResp.code}: ${modelResp.body?.string()?.take(150)}"
            }

            return@withContext ConnectionTestResult.Failure(modelResp.code, humanMsg)

        } catch (e: SocketTimeoutException) {
            return@withContext ConnectionTestResult.Failure(null, "Connection timed out connecting to Hugging Face.")
        } catch (e: Exception) {
            Log.e(TAG, "Hugging Face test connection error", e)
            return@withContext ConnectionTestResult.Failure(null, "Connection error: ${e.message}")
        }
    }
}
