package com.example.data

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

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiChatService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val systemInstruction = """
        You are BannerCraft AI, an expert YouTube Creative Director and Banner Prompt Engineer.
        Your goal is to help creators brainstorm channel concepts, branding aesthetics, color palettes,
        and generate vivid, ultra-detailed image prompts tailored for YouTube banners.
        
        Guidelines:
        1. Keep in mind the official YouTube Safe Zone: the central 1546 x 423 px area on a 2560 x 1440 px canvas.
        2. When suggesting an image prompt, format it clearly inside a block or label it as **[PROMPT]**, describing background visuals, mood, lighting, and composition.
        3. Be encouraging, concise, creative, and actionable!
    """.trimIndent()

    fun sanitizeApiKey(key: String): String {
        var cleaned = key.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .replace("\\s+".toRegex(), "")
        cleaned = cleaned.replace("(?i)^(api[-_]?key[:.]?|bearer)+".toRegex(), "")
        return cleaned.trimStart('.', ':', ';', ',', '-', ' ').trimEnd('.', ':', ';', ',', ' ')
    }

    /**
     * Normalizes conversation history into strict Gemini API structure:
     * 1. Skips leading assistant/greeting messages so turn 0 is ALWAYS role: "user".
     * 2. Merges consecutive messages of the same role into multiple parts of a single turn.
     * 3. Guarantees alternating roles (user -> model -> user -> model).
     */
    fun normalizeChatMessagesForGemini(messages: List<ChatMessage>): JSONArray {
        val contentsArray = JSONArray()
        val firstUserIndex = messages.indexOfFirst { it.isUser }
        if (firstUserIndex == -1) {
            // If only system greetings exist, create an initial user turn
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", "Hello BannerCraft AI!") })
                })
            })
            return contentsArray
        }

        val relevantMessages = messages.subList(firstUserIndex, messages.size)
        var currentRole: String? = null
        var currentParts = JSONArray()

        for (msg in relevantMessages) {
            val text = msg.text.trim()
            if (text.isBlank()) continue
            val role = if (msg.isUser) "user" else "model"

            if (role == currentRole) {
                // Same role consecutively -> combine as another part in this turn
                currentParts.put(JSONObject().apply { put("text", text) })
            } else {
                if (currentRole != null && currentParts.length() > 0) {
                    contentsArray.put(JSONObject().apply {
                        put("role", currentRole)
                        put("parts", currentParts)
                    })
                }
                currentRole = role
                currentParts = JSONArray().apply {
                    put(JSONObject().apply { put("text", text) })
                }
            }
        }

        if (currentRole != null && currentParts.length() > 0) {
            contentsArray.put(JSONObject().apply {
                put("role", currentRole)
                put("parts", currentParts)
            })
        }

        return contentsArray
    }

    /**
     * Sends conversation history to Gemini text model and returns assistant response.
     */
    suspend fun sendChat(
        apiKey: String,
        messages: List<ChatMessage>,
        model: String = "gemini-2.0-flash"
    ): String = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)

        if (cleanKey.isBlank()) {
            throw InvalidApiKeyException("Gemini API key is required. Please set it in Settings.")
        }

        if (cleanKey.startsWith("nvapi-", ignoreCase = true)) {
            throw InvalidApiKeyException("An NVIDIA NIM API key (nvapi-...) was passed to Google Gemini. Please switch to NVIDIA NIM or enter your Google AI Studio key (AIzaSy...).")
        }

        val primaryModel = model.trim().removePrefix("models/").ifBlank { "gemini-3.8-flash" }
        val candidateModels = listOf(
            primaryModel,
            "gemini-3.8-flash",
            "gemini-flash-latest",
            "gemini-3.7-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.1-pro-preview",
            "gemini-pro-latest",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
        ).filterNot { it.contains("2.5") || it.contains("1.5-pro-latest") }.distinct()

        val contentsArray = normalizeChatMessagesForGemini(messages)

        val payload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1500)
            })
        }

        var lastError: Exception? = null

        // Try primary and fallback models across v1beta and v1 endpoints
        for (targetModel in candidateModels) {
            val endpointVersions = listOf("v1beta", "v1")
            for (version in endpointVersions) {
                try {
                    val endpointUrl = "https://generativelanguage.googleapis.com/$version/models/$targetModel:generateContent?key=$cleanKey"
                    val (code, body) = executeRequestWithBackoff(endpointUrl, cleanKey, payload.toString(), targetModel)

                    when (code) {
                        in 200..299 -> {
                            val text = parseTextContent(body)
                            if (text.isNotBlank()) {
                                Log.i(TAG, "Chat successfully generated with model: '$targetModel' on $version")
                                return@withContext text
                            }
                        }
                        401, 403 -> {
                            val err = parseGoogleErrorMessage(body)
                            Log.e(TAG, "Authentication failed ($code): $err. Raw: $body")
                            throw InvalidApiKeyException("Invalid or unauthorized API key (HTTP $code): $err")
                        }
                        429 -> {
                            val err = parseGoogleErrorMessage(body)
                            Log.e(TAG, "Rate limit reached (429): $err")
                            throw RateLimitException("Google API Quota reached (HTTP 429): $err")
                        }
                        404 -> {
                            val err = parseGoogleErrorMessage(body)
                            Log.w(TAG, "Model '$targetModel' not found (404) on $version: $err")
                            lastError = GeminiApiException(code, "Model '$targetModel' not found (HTTP 404): $err", body)
                            continue
                        }
                        else -> {
                            val err = parseGoogleErrorMessage(body)
                            Log.e(TAG, "API call failed ($code): $err. Raw: $body")
                            lastError = GeminiApiException(code, "Chat failed with status $code: $err", body)
                        }
                    }
                } catch (e: InvalidApiKeyException) {
                    throw e
                } catch (e: RateLimitException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Attempt with model '$targetModel' on $version failed: ${e.message}")
                    lastError = e
                }
            }
        }

        // If candidate models returned 404, dynamically query ListModels to find any active chat model
        val discoveredModels = fetchAvailableGeminiModels(cleanKey)
        for (discModel in discoveredModels) {
            if (discModel in candidateModels) continue
            try {
                val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$discModel:generateContent?key=$cleanKey"
                val (code, body) = executeRequestWithBackoff(endpointUrl, cleanKey, payload.toString(), discModel)
                if (code in 200..299) {
                    val text = parseTextContent(body)
                    if (text.isNotBlank()) {
                        Log.i(TAG, "Chat successfully generated with dynamically discovered model: '$discModel'")
                        return@withContext text
                    }
                }
            } catch (_: Exception) {}
        }

        throw lastError ?: GeminiApiException(
            500,
            "Google Gemini API failed to generate response. Please verify that your Google API key has Generative Language API access enabled."
        )
    }

    /**
     * Discovers active models for this user's API key.
     */
    private suspend fun fetchAvailableGeminiModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1/models?key=$apiKey"
        )
        val badKeywords = listOf("tts", "transcribe", "robotics", "computer-use", "image", "banana", "2.5", "1.5")
        for (ep in endpoints) {
            try {
                val req = Request.Builder().url(ep).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val root = JSONObject(body)
                        val modelsArr = root.optJSONArray("models") ?: return@use
                        val supported = mutableListOf<String>()
                        for (i in 0 until modelsArr.length()) {
                            val mObj = modelsArr.getJSONObject(i)
                            val name = mObj.optString("name", "").removePrefix("models/")
                            val supportedMethods = mObj.optJSONArray("supportedGenerationMethods")
                            val supportsGenerate = if (supportedMethods != null) {
                                (0 until supportedMethods.length()).any {
                                    supportedMethods.getString(it) == "generateContent"
                                }
                            } else true

                            val isNotBad = badKeywords.none { name.contains(it, ignoreCase = true) }
                            if (supportsGenerate && isNotBad && name.contains("gemini", ignoreCase = true)) {
                                supported.add(name)
                            }
                        }
                        if (supported.isNotEmpty()) {
                            supported.sortBy { m ->
                                when {
                                    m.contains("3.8-flash") -> 0
                                    m.contains("flash-latest") -> 1
                                    m.contains("3.7-flash") -> 2
                                    m.contains("3.6-flash") -> 3
                                    m.contains("3.5-flash") -> 4
                                    m.contains("3.1-pro") -> 5
                                    m.contains("pro-latest") -> 6
                                    else -> 10
                                }
                            }
                            Log.i(TAG, "Discovered ${supported.size} Gemini models for user key: $supported")
                            return@withContext supported
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ListModels failed on $ep: ${e.message}")
            }
        }
        emptyList()
    }

    /**
     * 1-tap test connectivity for Google Gemini API key.
     */
    suspend fun testConnection(
        apiKey: String,
        model: String = "gemini-3.8-flash"
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext ConnectionTestResult.Failure(400, "Google Gemini API key cannot be empty.")
        }
        val startTime = System.currentTimeMillis()
        val primary = model.trim().removePrefix("models/").ifBlank { "gemini-3.8-flash" }

        val testPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "ping") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 5)
            })
        }

        val testEndpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/$primary:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
        )

        var lastFailureMsg = "Could not connect to Google Gemini"
        var lastStatus: Int? = null

        for (ep in testEndpoints) {
            try {
                val (code, body) = executeRequestWithBackoff(ep, cleanKey, testPayload.toString(), primary, maxRetries = 1)
                val latency = System.currentTimeMillis() - startTime
                if (code in 200..299) {
                    return@withContext ConnectionTestResult.Success(
                        latencyMs = latency,
                        message = "Connected to Google Gemini ($primary) in ${latency}ms!"
                    )
                }
                lastStatus = code
                val err = parseGoogleErrorMessage(body)
                lastFailureMsg = "HTTP $code: $err"
                if (code == 401 || code == 403) {
                    break
                }
            } catch (e: Exception) {
                lastFailureMsg = e.message ?: "Network error connecting to Gemini"
            }
        }

        ConnectionTestResult.Failure(lastStatus, lastFailureMsg)
    }

    private suspend fun executeRequestWithBackoff(
        endpointUrl: String,
        apiKey: String,
        jsonBody: String,
        modelName: String,
        maxRetries: Int = 2
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val requestBody = jsonBody.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("x-goog-api-key", apiKey)
            .post(requestBody)
            .build()

        var attempt = 0
        var lastCode = 0
        var lastBody = ""

        while (attempt < maxRetries) {
            attempt++
            try {
                val response = client.newCall(request).execute()
                response.use { resp ->
                    lastCode = resp.code
                    lastBody = resp.body?.string() ?: ""

                    if (resp.isSuccessful) {
                        return@withContext Pair(lastCode, lastBody)
                    }

                    Log.e(TAG, "Gemini call attempt $attempt failed for '$modelName' with HTTP $lastCode: $lastBody")

                    if ((lastCode == 429 || lastCode == 503) && attempt < maxRetries) {
                        val backoff = 1000L * attempt
                        delay(backoff)
                    } else {
                        return@withContext Pair(lastCode, lastBody)
                    }
                }
            } catch (e: IOException) {
                if (attempt < maxRetries) {
                    delay(1000L * attempt)
                } else {
                    throw IOException("Network error communicating with Google Gemini: ${e.message}", e)
                }
            }
        }

        Pair(lastCode, lastBody)
    }

    private fun parseTextContent(jsonResponse: String): String {
        return try {
            val root = JSONObject(jsonResponse)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""

            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val text = part.optString("text", "")
                sb.append(text)
            }
            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing chat response text", e)
            ""
        }
    }

    private fun parseGoogleErrorMessage(bodyString: String): String {
        return try {
            val root = JSONObject(bodyString)
            val errorObj = root.optJSONObject("error")
            errorObj?.optString("message") ?: bodyString.take(250)
        } catch (e: Exception) {
            bodyString.take(250)
        }
    }

    companion object {
        private const val TAG = "GeminiChatService"
    }
}
