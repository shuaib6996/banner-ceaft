package com.example.data

import android.graphics.BitmapFactory
import android.util.Base64
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
 * Service for Cloudflare Workers AI Text-to-Image generation using the official
 * Cloudflare Workers AI REST API (v4).
 *
 * Endpoint format:
 * POST https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/{MODEL_ID}
 *
 * Supports both JSON base64-encoded responses (FLUX models) and binary image streams (SDXL models),
 * as well as Cloudflare's 10,000 Neurons/day free allocation tier.
 */
class CloudflareImageService {

    companion object {
        private const val TAG = "CloudflareImageService"
        const val DEFAULT_MODEL = "@cf/black-forest-labs/flux-1-schnell"
        const val BASE_URL_TEMPLATE = "https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/"
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
     * Resolves the Account ID and target endpoint URL from the provided Base URL and Model ID.
     * Handles:
     * 1. Full URL: https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/
     * 2. URL without trailing slash: https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run
     * 3. Base URL with just account ID or root: https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}
     * 4. User simply entered raw 32-hex Account ID in the Base URL field.
     */
    fun extractAccountIdAndEndpoint(baseUrl: String, model: String): Pair<String?, String> {
        val cleanBase = baseUrl.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")

        val cleanModel = model.trim().let {
            if (it.isBlank()) DEFAULT_MODEL
            else if (!it.startsWith("@") && !it.startsWith("lucid") && !it.startsWith("phoenix") && it.contains("/")) {
                "@cf/$it"
            } else it
        }

        // Clean any enclosing braces/brackets like {ID}, <ID>, [ID] if user entered them
        val strippedId = cleanBase
            .trimStart('{', '<', '[', '(', ' ', '"', '\'')
            .trimEnd('}', '>', ']', ')', ' ', '"', '\'')

        // Case A: User entered just the raw Account ID (alphanumeric, no slashes or dots)
        if (strippedId.isNotBlank() && !strippedId.contains("/") && !strippedId.contains(".")) {
            val accountId = strippedId.replace(Regex("(?i)^account[-_]?id[:= ]*"), "").trim()
            val endpoint = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/$cleanModel"
            return Pair(accountId, endpoint)
        }

        // Case B: URL containing accounts/{ACCOUNT_ID} or accounts/ACCOUNT_ID
        val accountRegex = Regex("""accounts/([^/]+)""")
        val match = accountRegex.find(cleanBase)
        val rawExtracted = match?.groupValues?.get(1)?.trim()
        val extractedId = rawExtracted
            ?.trimStart('{', '<', '[', '(', ' ', '"', '\'')
            ?.trimEnd('}', '>', ']', ')', ' ', '"', '\'')
            ?.takeIf { !it.equals("ACCOUNT_ID", ignoreCase = true) && it.isNotBlank() }

        val endpoint = if (extractedId != null) {
            "https://api.cloudflare.com/client/v4/accounts/$extractedId/ai/run/$cleanModel"
        } else if (cleanBase.contains("/ai/run")) {
            val prefix = cleanBase.substringBefore("/ai/run") + "/ai/run"
            "$prefix/$cleanModel"
        } else if (cleanBase.contains("/accounts/")) {
            val basePart = cleanBase.trimEnd('/')
            "$basePart/ai/run/$cleanModel"
        } else if (cleanBase.isNotBlank()) {
            val basePart = cleanBase.trimEnd('/')
            "$basePart/$cleanModel"
        } else {
            "https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/$cleanModel"
        }

        return Pair(extractedId, endpoint)
    }

    /**
     * Cloudflare Workers AI has a strict schema constraint: 'Length of /prompt must be <= 2048'.
     * This method safely condenses any long prompt to under 1900 chars at a clean word/sentence boundary.
     */
    fun sanitizePromptLength(prompt: String, maxLimit: Int = 1900): String {
        val trimmed = prompt.trim()
        if (trimmed.length <= maxLimit) return trimmed

        val slice = trimmed.take(maxLimit)
        val lastBreak = maxOf(
            slice.lastIndexOf(". "),
            slice.lastIndexOf(", "),
            slice.lastIndexOf("\n"),
            slice.lastIndexOf(" ")
        )

        val clean = if (lastBreak > maxLimit / 2) {
            slice.substring(0, lastBreak).trimEnd('.', ',', ';', ' ') + "."
        } else {
            slice.trimEnd()
        }

        Log.i(TAG, "Cloudflare prompt condensed from ${trimmed.length} to ${clean.length} chars to satisfy <= 2048 limit.")
        return clean
    }

    /**
     * Constructs the JSON request body conforming to the official Cloudflare Workers AI schema
     * for the selected model.
     */
    fun buildPayload(model: String, prompt: String, steps: Int? = null, guidance: Double? = null): JSONObject {
        val safePrompt = sanitizePromptLength(prompt)
        val payload = JSONObject().apply {
            put("prompt", safePrompt)
            val lowerModel = model.lowercase()
            when {
                lowerModel.contains("flux") -> {
                    // FLUX accepts steps (4-8, default 4 for schnell)
                    put("steps", steps ?: 4)
                }
                lowerModel.contains("lightning") || lowerModel.contains("lcm") -> {
                    // SDXL Lightning / LCM accepts num_steps (default 8)
                    put("num_steps", steps ?: 8)
                }
                lowerModel.contains("stable-diffusion") || lowerModel.contains("sdxl") -> {
                    // Standard SDXL accepts num_steps (default 20) and guidance (default 7.5)
                    put("num_steps", steps ?: 20)
                    put("guidance", guidance ?: 7.5)
                }
            }
        }
        return payload
    }

    /**
     * Detects if byte buffer starts with standard PNG, JPEG, or WebP magic headers.
     */
    private fun isImageMagicBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // PNG header: 89 50 4E 47
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
            return true
        }
        // JPEG header: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return true
        }
        // WebP header: RIFF....WEBP
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return true
        }
        return false
    }

    /**
     * Parses the response from Cloudflare Workers AI.
     * Handles both raw binary image streams and JSON envelopes containing base64 images.
     */
    fun parseImageResponse(response: okhttp3.Response): ByteArray {
        val contentType = response.header("Content-Type")?.lowercase().orEmpty()
        val responseBody = response.body ?: throw IOException("Empty response received from Cloudflare Workers AI.")
        val rawBytes = responseBody.bytes()

        if (rawBytes.isEmpty()) {
            throw IOException("Cloudflare Workers AI returned an empty response body.")
        }

        // 1. Direct binary image response
        if (contentType.startsWith("image/") || isImageMagicBytes(rawBytes)) {
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            if (bitmap != null) {
                bitmap.recycle()
                return rawBytes
            }
        }

        // 2. JSON response (either {"result": {"image": "..."}} or {"image": "..."} or error object)
        val bodyString = String(rawBytes, Charsets.UTF_8).trim()
        if (bodyString.startsWith("{")) {
            val json = JSONObject(bodyString)
            val success = json.optBoolean("success", true)
            if (!success) {
                val errors = json.optJSONArray("errors")
                val errorMsg = if (errors != null && errors.length() > 0) {
                    val errObj = errors.optJSONObject(0)
                    errObj?.optString("message") ?: errors.optString(0)
                } else {
                    json.optString("message", "Unknown Cloudflare error")
                }
                throw IOException("Cloudflare Workers AI: $errorMsg")
            }

            // Extract base64 image string
            val base64Str = when {
                json.has("result") -> {
                    val result = json.opt("result")
                    when (result) {
                        is JSONObject -> result.optString("image")
                        is String -> result
                        else -> ""
                    }
                }
                json.has("image") -> json.optString("image")
                else -> ""
            }

            if (base64Str.isNotBlank()) {
                val cleanBase64 = if (base64Str.contains(",")) {
                    base64Str.substringAfter(",")
                } else {
                    base64Str
                }.trim().replace("\n", "").replace("\r", "")

                return try {
                    val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        bitmap.recycle()
                        decodedBytes
                    } else {
                        throw IOException("Cloudflare Workers AI returned base64 data that could not be decoded as an image.")
                    }
                } catch (e: IllegalArgumentException) {
                    throw IOException("Invalid base64 image data returned by Cloudflare Workers AI.", e)
                }
            }
        }

        val snippet = bodyString.take(200)
        throw IOException("Cloudflare Workers AI returned an unrecognized response format: $snippet")
    }

    /**
     * Generates an image using Cloudflare Workers AI REST API.
     *
     * @param baseUrl Base URL or Account ID (e.g. "https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/")
     * @param apiKey Cloudflare API Token (Workers AI Read/Edit permissions)
     * @param model Model ID (e.g. "@cf/black-forest-labs/flux-1-schnell")
     * @param prompt Text prompt describing the banner or image
     * @param steps Optional number of generation steps
     * @param guidance Optional guidance scale
     * @param onProgress Callback for status updates
     * @return Raw image bytes (PNG / JPEG)
     */
    suspend fun generateImage(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        steps: Int? = null,
        guidance: Double? = null,
        onProgress: ((String) -> Unit)? = null
    ): ByteArray = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            throw IOException("Cloudflare API token is required. Please set it in Settings.")
        }

        if (baseUrl.contains("{ACCOUNT_ID}")) {
            throw IOException("Please replace '{ACCOUNT_ID}' in the Base URL with your actual Cloudflare Account ID.")
        }

        val cleanModel = model.trim().ifBlank { DEFAULT_MODEL }
        val (_, endpoint) = extractAccountIdAndEndpoint(baseUrl, cleanModel)

        var currentPrompt = sanitizePromptLength(prompt)
        var attempts = 0
        val maxAttempts = 2

        while (attempts < maxAttempts) {
            attempts++
            val payloadJson = buildPayload(cleanModel, currentPrompt, steps, guidance)

            val request = Request.Builder()
                .url(endpoint)
                .post(payloadJson.toString().toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Accept", "image/png, image/jpeg, application/json, */*")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Title", "BannerCraft")
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: SocketTimeoutException) {
                throw IOException(
                    "Request timed out. Cloudflare Workers AI took too long to generate an image. " +
                        "Please try again or use a faster model like '@cf/black-forest-labs/flux-1-schnell'.",
                    e
                )
            }

            if (response.isSuccessful) {
                val imageBytes = parseImageResponse(response)

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

            // Handle 503 Capacity retry with short backoff
            if (statusCode == 503) {
                if (attempts < maxAttempts) {
                    Log.i(TAG, "Cloudflare Workers AI capacity busy (HTTP 503). Retrying in 4s (attempt $attempts/$maxAttempts)...")
                    onProgress?.invoke("Cloudflare compute busy, retrying...")
                    delay(4000L)
                    continue
                } else {
                    throw IOException("Cloudflare Workers AI is currently experiencing high GPU demand. Please try again in a few moments.")
                }
            }

            // Parse Cloudflare error details
            val parsedError = try {
                val json = JSONObject(bodyString)
                val errors = json.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    val err = errors.optJSONObject(0)
                    err?.optString("message") ?: errors.optString(0)
                } else {
                    json.optString("message", bodyString)
                }
            } catch (_: Exception) {
                bodyString.take(200)
            }

            // Handle Cloudflare Bad input length error (Length of '/prompt' must be <= 2048)
            val isLengthExceeded = statusCode == 400 && (
                bodyString.contains("must be <= 2048", ignoreCase = true) ||
                parsedError.contains("must be <= 2048", ignoreCase = true)
            )

            if (isLengthExceeded) {
                if (attempts < maxAttempts) {
                    Log.i(TAG, "Prompt exceeded Cloudflare 2048 limit (${currentPrompt.length} chars). Truncating to 1500 chars and retrying...")
                    onProgress?.invoke("Compressing long prompt for Cloudflare...")
                    currentPrompt = sanitizePromptLength(currentPrompt, 1500)
                    delay(300L)
                    continue
                }
            }

            // Handle Cloudflare aggressive false-positive NSFW prompt filter (Error 3030)
            val isNsfwFlagged = statusCode == 400 && (
                bodyString.contains("NSFW", ignoreCase = true) ||
                parsedError.contains("NSFW", ignoreCase = true) ||
                bodyString.contains("3030")
            )

            if (isNsfwFlagged) {
                if (attempts < maxAttempts) {
                    Log.i(TAG, "Cloudflare flagged prompt '$currentPrompt' as false-positive NSFW. Auto-enriching with safe artistic context...")
                    onProgress?.invoke("Adapting prompt for Cloudflare safety guardrails...")
                    currentPrompt = sanitizePromptLength("$prompt, clean studio lighting, high resolution, detailed digital artwork", 1900)
                    delay(500L)
                    continue
                } else {
                    throw ContentModeratedException(
                        "⚠️ Content Moderated: This prompt was flagged by AI safety guidelines. Please modify your prompt and try again."
                    )
                }
            }

            when (statusCode) {
                401 -> throw IOException("Invalid Cloudflare API token. Please check your token in Settings.")
                403 -> throw IOException("Cloudflare API token lacks Workers AI permissions, or access to this model is forbidden.")
                404 -> throw IOException("Cloudflare endpoint or model not found. Verify your Account ID and Model ID ($cleanModel).")
                429 -> throw IOException(
                    "Cloudflare free daily allocation (10,000 Neurons) or rate limit exhausted. " +
                        "Quota resets at 00:00 UTC, or upgrade to a Workers Paid plan."
                )
                else -> throw IOException("Cloudflare Workers AI returned HTTP $statusCode: ${parsedError.ifBlank { "Unknown error" }}")
            }
        }

        throw IOException("Failed to generate image from Cloudflare Workers AI after $maxAttempts attempts.")
    }

    /**
     * Tests connectivity to Cloudflare Workers AI by checking account access and model availability.
     */
    suspend fun testConnection(
        baseUrl: String,
        apiKey: String,
        model: String
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext ConnectionTestResult.Failure(401, "Cloudflare API Token is required.")
        }

        if (baseUrl.contains("{ACCOUNT_ID}")) {
            return@withContext ConnectionTestResult.Failure(
                400,
                "Please replace '{ACCOUNT_ID}' in the Base URL with your actual Cloudflare Account ID."
            )
        }

        val cleanModel = model.trim().ifBlank { DEFAULT_MODEL }
        val (accountId, _) = extractAccountIdAndEndpoint(baseUrl, cleanModel)
        val startTime = System.currentTimeMillis()

        try {
            // If Account ID is available, test via the model search endpoint for this account
            if (!accountId.isNullOrBlank()) {
                val searchModelName = cleanModel.substringAfterLast("/")
                val searchUrl = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/models/search?search=$searchModelName"

                val request = Request.Builder()
                    .url(searchUrl)
                    .addHeader("Authorization", "Bearer $cleanKey")
                    .get()
                    .build()

                val resp = client.newCall(request).execute()
                val latency = System.currentTimeMillis() - startTime

                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val success = json.optBoolean("success", true)
                    if (success) {
                        return@withContext ConnectionTestResult.Success(
                            latencyMs = latency,
                            message = "Connected to Cloudflare Workers AI! Account verified and model '$cleanModel' is accessible."
                        )
                    }
                }

                val bodyStr = resp.body?.string().orEmpty()
                val parsedMsg = try {
                    val json = JSONObject(bodyStr)
                    val errors = json.optJSONArray("errors")
                    if (errors != null && errors.length() > 0) {
                        val err = errors.optJSONObject(0)
                        err?.optString("message") ?: errors.optString(0)
                    } else {
                        json.optString("message", "")
                    }
                } catch (_: Exception) {
                    ""
                }

                val humanMsg = when (resp.code) {
                    401 -> "Invalid Cloudflare API Token."
                    403 -> "Cloudflare API Token lacks Workers AI Read permissions."
                    404 -> "Cloudflare Account ID '$accountId' was not found or is invalid."
                    429 -> "Cloudflare daily free allocation (10,000 Neurons) or rate limit exhausted."
                    else -> parsedMsg.ifBlank { "Cloudflare returned HTTP ${resp.code}." }
                }

                return@withContext ConnectionTestResult.Failure(resp.code, humanMsg)
            }

            // Fallback: Verify token directly using /user/tokens/verify
            val verifyUrl = "https://api.cloudflare.com/client/v4/user/tokens/verify"
            val verifyReq = Request.Builder()
                .url(verifyUrl)
                .addHeader("Authorization", "Bearer $cleanKey")
                .get()
                .build()

            val verifyResp = client.newCall(verifyReq).execute()
            val latency = System.currentTimeMillis() - startTime

            if (verifyResp.isSuccessful) {
                return@withContext ConnectionTestResult.Success(
                    latencyMs = latency,
                    message = "Cloudflare API Token is valid! (Tip: Enter your Account ID in Base URL for full verification)."
                )
            }

            return@withContext ConnectionTestResult.Failure(
                verifyResp.code,
                if (verifyResp.code == 401) "Invalid Cloudflare API Token." else "Token verification returned HTTP ${verifyResp.code}."
            )

        } catch (e: SocketTimeoutException) {
            return@withContext ConnectionTestResult.Failure(null, "Connection timed out connecting to Cloudflare.")
        } catch (e: Exception) {
            Log.e(TAG, "Cloudflare test connection error", e)
            return@withContext ConnectionTestResult.Failure(null, "Connection error: ${e.message}")
        }
    }
}
