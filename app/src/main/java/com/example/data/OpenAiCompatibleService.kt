package com.example.data

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ConnectionTestResult {
    data class Success(val latencyMs: Long, val message: String) : ConnectionTestResult()
    data class Failure(val statusCode: Int?, val errorMessage: String) : ConnectionTestResult()
}

class OpenAiCompatibleService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun normalizeBaseUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.dropLast(1)
        }
        return trimmed
    }

    fun sanitizeApiKey(rawKey: String): String {
        var key = rawKey.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .replace("\\s+".toRegex(), "")
        // Strip accidental user prefixes like 'api key.', 'bearer'
        key = key.replace("(?i)^(api[-_]?key[:.]?|bearer)+".toRegex(), "")
        // Strip accidental punctuation or dots
        return key.trimStart('.', ':', ';', ',', '-', ' ').trimEnd('.', ':', ';', ',', ' ')
    }

    /**
     * Tests connectivity to an OpenAI-compatible endpoint.
     * Tries a minimal 1-token completion or /models probe, and calculates latency.
     */
    suspend fun testConnection(
        baseUrl: String,
        apiKey: String,
        model: String
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val cleanUrl = normalizeBaseUrl(baseUrl)
        val cleanKey = sanitizeApiKey(apiKey)
        val cleanModel = model.trim().ifBlank { "gpt-4o-mini" }

        val startTime = System.currentTimeMillis()

        try {
            // First try a quick chat completion probe with 1 token
            val endpoint = "$cleanUrl/chat/completions"
            val requestBodyJson = JSONObject().apply {
                put("model", cleanModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
                put("max_tokens", 2)
            }

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")

            if (cleanKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $cleanKey")
            }
            requestBuilder.addHeader("HTTP-Referer", "https://bannercraft.ai")
            requestBuilder.addHeader("X-Title", "BannerCraft")

            val response = client.newCall(requestBuilder.build()).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseCode = response.code
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                return@withContext ConnectionTestResult.Success(
                    latencyMs = latency,
                    message = "Connected to $cleanModel in ${latency}ms!"
                )
            }

            var finalCode = responseCode
            var finalBody = bodyString

            // If 404 on /chat/completions (e.g. testing with an image model), try /models probe
            if (responseCode == 404) {
                val modelsEndpoint = "$cleanUrl/models"
                val modelsReq = Request.Builder()
                    .url(modelsEndpoint)
                    .get()
                if (cleanKey.isNotBlank()) {
                    modelsReq.addHeader("Authorization", "Bearer $cleanKey")
                }
                val modelsResp = client.newCall(modelsReq.build()).execute()
                val modelsLatency = System.currentTimeMillis() - startTime
                val modelsBody = modelsResp.body?.string().orEmpty()
                if (modelsResp.isSuccessful) {
                    return@withContext ConnectionTestResult.Success(
                        latencyMs = modelsLatency,
                        message = "Connected to provider in ${modelsLatency}ms!"
                    )
                } else {
                    finalCode = modelsResp.code
                    finalBody = modelsBody
                }
            }

            // Extract error message from JSON response if present
            val parsedError = try {
                val json = JSONObject(finalBody)
                val errObj = json.optJSONObject("error")
                errObj?.optString("message") ?: json.optString("detail", json.optString("message", finalBody))
            } catch (e: Exception) {
                finalBody.take(200)
            }

            val humanMsg = when (finalCode) {
                401, 403 -> "Authentication failed ($finalCode). Check your API Key."
                404 -> "Endpoint not found ($finalCode). Check your Base URL (e.g. should include /v1)."
                429 -> "Rate limit reached (429). Check provider quota."
                else -> "Server error ($finalCode): ${parsedError.ifBlank { "Unknown" }}"
            }

            return@withContext ConnectionTestResult.Failure(finalCode, humanMsg)

        } catch (e: IOException) {
            Log.e(TAG, "Connection test network error", e)
            return@withContext ConnectionTestResult.Failure(
                statusCode = null,
                errorMessage = "Network error: ${e.localizedMessage ?: "Could not connect to host"}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Connection test exception", e)
            return@withContext ConnectionTestResult.Failure(
                statusCode = null,
                errorMessage = "Error: ${e.message}"
            )
        }
    }

    /**
     * Sends conversation to standard OpenAI chat completions endpoint.
     */
    suspend fun sendChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String = "You are BannerCraft AI, an expert YouTube Creative Director and Banner Prompt Engineer. Help the user brainstorm YouTube banner concepts and detailed prompts."
    ): String = withContext(Dispatchers.IO) {
        val cleanUrl = normalizeBaseUrl(baseUrl)
        val cleanKey = sanitizeApiKey(apiKey)
        val cleanModel = model.trim().ifBlank { "gpt-4o-mini" }

        val endpoint = "$cleanUrl/chat/completions"

        val messagesArray = JSONArray()

        // System prompt
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        // Conversation history
        messages.takeLast(10).forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "assistant")
                put("content", msg.text)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", cleanModel)
            put("messages", messagesArray)
            put("temperature", 0.7)
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")

        if (cleanKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $cleanKey")
        }
        requestBuilder.addHeader("HTTP-Referer", "https://bannercraft.ai")
        requestBuilder.addHeader("X-Title", "BannerCraft")

        val response = try {
            client.newCall(requestBuilder.build()).execute()
        } catch (e: java.net.SocketTimeoutException) {
            throw IOException("Request timed out. The model '$cleanModel' took too long to respond on this endpoint. Try switching to a faster model like 'meta/llama-3.2-11b-vision-instruct'.", e)
        }
        val bodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val parsedMsg = try {
                JSONObject(bodyString).optJSONObject("error")?.optString("message") ?: bodyString
            } catch (e: Exception) {
                bodyString.take(200)
            }
            if (response.code == 401 || response.code == 403) {
                throw IOException("Authentication failed (HTTP ${response.code}): Invalid API key. Please check your token.")
            }
            if (response.code == 404) {
                throw IOException("HTTP 404 Not Found: Model '$cleanModel' is not recognized by this endpoint. For NVIDIA NIM, please use 'meta/llama-3.2-11b-vision-instruct'. (Details: $parsedMsg)")
            }
            throw IOException("Provider returned HTTP ${response.code}: $parsedMsg")
        }

        val json = JSONObject(bodyString)
        val choices = json.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw IOException("No response choices returned by provider.")
        }

        val firstChoice = choices.getJSONObject(0)
        val msgObj = firstChoice.optJSONObject("message")
        val content = msgObj?.optString("content")?.takeIf { it.isNotBlank() && it != "null" }
            ?: msgObj?.optString("reasoning_content")?.takeIf { it.isNotBlank() && it != "null" }
            ?: msgObj?.optString("reasoning")?.takeIf { it.isNotBlank() && it != "null" }
            ?: firstChoice.optString("text").takeIf { it.isNotBlank() && it != "null" }

        if (content.isNullOrBlank()) {
            throw IOException("Empty response text from AI provider.")
        }

        return@withContext content.trim()
    }

    /**
     * Generates an image using either NVIDIA NIM hosted GenAI endpoints (FLUX.1)
     * or standard OpenAI /images/generations endpoint for OpenRouter, Together, OpenAI, etc.
     */
    suspend fun generateImage(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        size: String = "1024x1024"
    ): ByteArray = withContext(Dispatchers.IO) {
        val cleanUrl = normalizeBaseUrl(baseUrl)
        val cleanKey = sanitizeApiKey(apiKey)
        val cleanModel = model.trim().ifBlank { "black-forest-labs/flux.1-dev" }

        val isNvidia = cleanKey.startsWith("nvapi-", ignoreCase = true) ||
            cleanUrl.contains("nvidia", ignoreCase = true) ||
            cleanModel.contains("flux", ignoreCase = true)

        if (isNvidia) {
            return@withContext generateNvidiaImage(cleanKey, cleanModel, prompt)
        }

        val endpoint = "$cleanUrl/images/generations"

        val requestJson = JSONObject().apply {
            put("model", cleanModel)
            put("prompt", prompt)
            put("n", 1)
            put("size", size)
            put("response_format", "b64_json")
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")

        if (cleanKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $cleanKey")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val bodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val parsedMsg = try {
                JSONObject(bodyString).optJSONObject("error")?.optString("message") ?: bodyString
            } catch (e: Exception) {
                bodyString.take(200)
            }
            if (response.code == 404) {
                throw IOException("HTTP 404 Not Found: The image endpoint '$endpoint' is not available on this provider. For NVIDIA NIM, FLUX.1 Dev is used; for Google, Imagen 3 is used.")
            }
            throw IOException("Image Generation failed (HTTP ${response.code}): $parsedMsg")
        }

        val json = JSONObject(bodyString)
        val dataArray = json.optJSONArray("data")
        if (dataArray == null || dataArray.length() == 0) {
            throw IOException("Provider did not return image data.")
        }

        val firstItem = dataArray.getJSONObject(0)
        val finishReason = firstItem.optString("finish_reason", "")
        if (finishReason.equals("CONTENT_FILTER", ignoreCase = true) ||
            finishReason.equals("SAFETY", ignoreCase = true)
        ) {
            throw ContentModeratedException("⚠️ Content Moderated: The image generation was blocked by AI safety guidelines ($finishReason). Please modify your prompt.")
        }

        val b64 = firstItem.optString("b64_json")
        if (b64.isNotBlank()) {
            val decoded = Base64.decode(b64, Base64.DEFAULT)
            ImageSafetyValidator.validateImage(decoded)
            return@withContext decoded
        }

        // If returned as direct image URL, download image
        val imageUrl = firstItem.optString("url")
        if (imageUrl.isNotBlank()) {
            val imgReq = Request.Builder().url(imageUrl).build()
            val imgResp = client.newCall(imgReq).execute()
            if (imgResp.isSuccessful) {
                val bytes = imgResp.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    ImageSafetyValidator.validateImage(bytes)
                    return@withContext bytes
                }
            }
        }

        throw IOException("No image bytes found in provider response.")
    }

    private suspend fun generateNvidiaImage(
        apiKey: String,
        model: String,
        prompt: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            throw IOException("NVIDIA NIM requires an API key (nvapi-...). Please add your NVIDIA API key in Settings.")
        }

        // Clean prompt to strip sensitive layout words that trigger NeMo Guardrails / FLUX filters
        val cleanPrompt = prompt
            .replace(Regex("(?i)\\b(heads?|cut off|chopped|completely empty|negative space|letterbox format|bleed|gore|kill|dead|corpse|slay)\\b"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        val targetModel = when {
            model.contains("schnell", ignoreCase = true) -> "black-forest-labs/flux.1-schnell"
            model.contains("kontext", ignoreCase = true) -> "black-forest-labs/flux.1-kontext-dev"
            model.contains("klein", ignoreCase = true) -> "black-forest-labs/flux.2-klein-4b"
            model.contains("turbo", ignoreCase = true) || model.contains("sdxl", ignoreCase = true) -> "stabilityai/sdxl-turbo"
            model.contains("medium", ignoreCase = true) -> "stabilityai/stable-diffusion-3-medium"
            else -> "black-forest-labs/flux.1-dev"
        }

        // Only use verified active NVIDIA GenAI endpoints on ai.api.nvidia.com
        val attempts = listOf(
            Pair("https://ai.api.nvidia.com/v1/genai/$targetModel", targetModel),
            Pair("https://ai.api.nvidia.com/v1/genai/black-forest-labs/flux.1-dev", "black-forest-labs/flux.1-dev"),
            Pair("https://ai.api.nvidia.com/v1/genai/black-forest-labs/flux.1-schnell", "black-forest-labs/flux.1-schnell")
        ).distinctBy { it.first }

        var lastErrorMsg = "Failed to generate image with NVIDIA NIM"
        var isModeratedFlag = false

        for ((endpoint, modelId) in attempts) {
            try {
                val requestJson = JSONObject().apply {
                    put("prompt", cleanPrompt)
                    put("mode", "base")
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestJson.toString().toRequestBody(jsonMediaType))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer $cleanKey")
                    .build()

                val response = client.newCall(request).execute()
                val bodyString = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val root = JSONObject(bodyString)
                    val artifacts = root.optJSONArray("artifacts")
                    if (artifacts != null && artifacts.length() > 0) {
                        val artifact = artifacts.getJSONObject(0)
                        val finishReason = artifact.optString("finish_reason", "")
                        if (finishReason.contains("FILTER", ignoreCase = true) ||
                            finishReason.contains("SAFETY", ignoreCase = true)
                        ) {
                            isModeratedFlag = true
                            Log.w(TAG, "NVIDIA FLUX safety guardrail triggered ($finishReason) on $endpoint. Attempting fallback...")
                            continue // Seamlessly attempt fallback instead of failing immediately!
                        }

                        val b64 = artifact.optString("base64")
                        if (b64.isNotBlank()) {
                            val decoded = Base64.decode(b64, Base64.DEFAULT)
                            ImageSafetyValidator.validateImage(decoded)
                            Log.i(TAG, "Successfully generated and verified visible NVIDIA banner image with $endpoint")
                            return@withContext decoded
                        }
                    }
                } else {
                    val parsedMsg = try {
                        val errObj = JSONObject(bodyString)
                        errObj.optString("detail", errObj.optString("title", bodyString.take(200)))
                    } catch (_: Exception) {
                        bodyString.take(200)
                    }

                    // Authentication failures (401 / 403) - do NOT retry, report immediately!
                    if (response.code == 401 || response.code == 403) {
                        Log.e(TAG, "NVIDIA NIM Auth failed (${response.code}): $bodyString")
                        throw IOException("NVIDIA NIM (${response.code}): API key invalid, unauthorized, or expired. Please check your nvapi-... key in Settings.")
                    }

                    // Rate limit
                    if (response.code == 429) {
                        Log.w(TAG, "NVIDIA NIM Rate limited (${response.code}): $bodyString")
                        throw IOException("NVIDIA NIM Rate Limit (429): Quota exceeded or too many requests. Please wait a moment and try again.")
                    }

                    if (response.code == 422 || parsedMsg.contains("safety", ignoreCase = true) || parsedMsg.contains("moderated", ignoreCase = true)) {
                        isModeratedFlag = true
                    }
                    lastErrorMsg = "NVIDIA NIM Image Generation (HTTP ${response.code}): $parsedMsg"
                    Log.w(TAG, "NVIDIA endpoint $endpoint failed with HTTP ${response.code}: $bodyString")
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastErrorMsg = "NVIDIA NIM image generation timed out. Try a shorter prompt."
                Log.w(TAG, "NVIDIA endpoint $endpoint timed out", e)
            } catch (e: ContentModeratedException) {
                isModeratedFlag = true
                Log.w(TAG, "NVIDIA endpoint $endpoint moderated, attempting fallback...", e)
            } catch (e: IOException) {
                // Re-throw our explicit auth/quota exceptions immediately
                if (e.message?.contains("API key invalid") == true ||
                    e.message?.contains("requires an API key") == true ||
                    e.message?.contains("Rate Limit") == true
                ) {
                    throw e
                }
                lastErrorMsg = e.message ?: "Network error calling NVIDIA NIM"
                Log.w(TAG, "NVIDIA endpoint $endpoint threw IOException", e)
            } catch (e: Exception) {
                lastErrorMsg = "Network error calling NVIDIA NIM: ${e.message}"
                Log.w(TAG, "NVIDIA endpoint $endpoint threw exception", e)
            }
        }

        if (isModeratedFlag) {
            throw ContentModeratedException(
                "⚠️ Content Moderated: NVIDIA safety guidelines flagged this prompt. Please modify or soften your prompt description and try again."
            )
        }

        throw IOException(lastErrorMsg)
    }

    companion object {
        private const val TAG = "OpenAiCompatibleService"
    }
}
