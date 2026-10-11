package com.example.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecureStorageManager(context: Context) {

    private val applicationContext = context.applicationContext

    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                applicationContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences, falling back to clean store", e)
            // Fallback in case of keystore corruption or read failure
            applicationContext.getSharedPreferences(PREFS_FALLBACK_FILE, Context.MODE_PRIVATE)
        }
    }

    suspend fun saveGoogleApiKey(key: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val trimmedKey = key.trim()
            if (trimmedKey.startsWith("nvapi-", ignoreCase = true)) {
                Log.w(TAG, "Attempted to save NVIDIA key as Google API key, ignoring")
                return@withContext false
            }
            prefs.edit()
                .putString(KEY_GOOGLE_API_KEY, trimmedKey)
                .putString(KEY_GEMINI_API_KEY, trimmedKey)
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write Google API key to secure storage", e)
            false
        }
    }

    suspend fun loadGoogleApiKey(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val dedicated = prefs.getString(KEY_GOOGLE_API_KEY, null)?.trim()
            if (!dedicated.isNullOrBlank() && !dedicated.startsWith("nvapi-", ignoreCase = true)) {
                return@withContext dedicated
            }
            val legacy = prefs.getString(KEY_GEMINI_API_KEY, null)?.trim()
            if (!legacy.isNullOrBlank() && !legacy.startsWith("nvapi-", ignoreCase = true)) {
                legacy
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read Google API key from secure storage", e)
            null
        }
    }

    suspend fun saveNvidiaApiKey(key: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val trimmedKey = key.trim()
            prefs.edit().putString(KEY_NVIDIA_API_KEY, trimmedKey).apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write NVIDIA API key to secure storage", e)
            false
        }
    }

    suspend fun loadNvidiaApiKey(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            prefs.getString(KEY_NVIDIA_API_KEY, null)?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun saveApiKey(key: String): Boolean = withContext(Dispatchers.IO) {
        val trimmedKey = key.trim()
        if (trimmedKey.startsWith("nvapi-", ignoreCase = true)) {
            saveNvidiaApiKey(trimmedKey)
        } else {
            saveGoogleApiKey(trimmedKey)
        }
    }

    suspend fun loadApiKey(): String? = withContext(Dispatchers.IO) {
        loadGoogleApiKey()
    }

    suspend fun clearApiKey(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            prefs.edit()
                .remove(KEY_GEMINI_API_KEY)
                .remove(KEY_GOOGLE_API_KEY)
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear API key from secure storage", e)
            false
        }
    }

    suspend fun saveSelectedModel(modelId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            prefs.edit().putString(KEY_SELECTED_MODEL, modelId.trim()).apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write selected model to secure storage", e)
            false
        }
    }

    suspend fun loadSelectedModel(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            prefs.getString(KEY_SELECTED_MODEL, AiModelConfig.DEFAULT_MODEL) ?: AiModelConfig.DEFAULT_MODEL
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read selected model from secure storage", e)
            AiModelConfig.DEFAULT_MODEL
        }
    }

    /**
     * Saves full OpenAI-compatible / Gemini provider configuration.
     */
    suspend fun saveProviderConfig(config: ApiProviderConfig): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            prefs.edit()
                .putString(KEY_PROVIDER_ID, config.providerId)
                .putString(KEY_PROVIDER_NAME, config.providerName)
                .putString(KEY_BASE_URL, config.baseUrl)
                .putString(KEY_GEMINI_API_KEY, config.apiKey)
                .putString(KEY_CHAT_MODEL, config.chatModel)
                .putString(KEY_IMAGE_MODEL, config.imageModel)
                .putBoolean(KEY_IS_OPENAI_COMPATIBLE, config.isOpenAiCompatible)
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write provider config to secure storage", e)
            false
        }
    }

    /**
     * Loads full provider configuration with fallback to presets or saved keys.
     */
    suspend fun loadProviderConfig(): ApiProviderConfig = withContext(Dispatchers.IO) {
        return@withContext try {
            val providerId = prefs.getString(KEY_PROVIDER_ID, null)
            val apiKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""

            if (providerId == null) {
                ApiProviderConfig(
                    providerId = "gemini",
                    providerName = "Google Gemini (Direct)",
                    baseUrl = "https://generativelanguage.googleapis.com",
                    apiKey = apiKey,
                    chatModel = "gemini-2.0-flash",
                    imageModel = "imagen-3.0-generate-002",
                    isOpenAiCompatible = false
                )
            } else {
                val preset = ApiProviderPresets.getPresetById(providerId)
                ApiProviderConfig(
                    providerId = providerId,
                    providerName = prefs.getString(KEY_PROVIDER_NAME, preset.name) ?: preset.name,
                    baseUrl = prefs.getString(KEY_BASE_URL, preset.baseUrl) ?: preset.baseUrl,
                    apiKey = apiKey,
                    chatModel = prefs.getString(KEY_CHAT_MODEL, preset.defaultChatModel) ?: preset.defaultChatModel,
                    imageModel = prefs.getString(KEY_IMAGE_MODEL, preset.defaultImageModel) ?: preset.defaultImageModel,
                    isOpenAiCompatible = prefs.getBoolean(KEY_IS_OPENAI_COMPATIBLE, preset.isOpenAiCompatible)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read provider config from secure storage", e)
            ApiProviderConfig()
        }
    }

    /**
     * Saves full list of provider profiles and marks the active profile ID.
     * Also keeps legacy individual preference keys in sync for backward compatibility.
     */
    suspend fun saveAllProviderProfiles(
        profiles: List<ApiProviderConfig>,
        activeProfileId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonArray = org.json.JSONArray()
            profiles.forEach { profile ->
                jsonArray.put(profile.toJson())
            }
            val activeId = activeProfileId ?: profiles.firstOrNull()?.id ?: ""
            val editor = prefs.edit()
            editor.putString(KEY_PROVIDER_PROFILES_JSON, jsonArray.toString())
            editor.putString(KEY_ACTIVE_PROFILE_ID, activeId)

            // Safely synchronize dedicated keys without trampling
            val geminiConfig = profiles.find { it.providerId == "gemini" }
            if (geminiConfig != null && geminiConfig.apiKey.isNotBlank() && !geminiConfig.apiKey.startsWith("nvapi-", ignoreCase = true)) {
                editor.putString(KEY_GOOGLE_API_KEY, geminiConfig.apiKey)
                editor.putString(KEY_GEMINI_API_KEY, geminiConfig.apiKey)
            }

            val nvidiaConfig = profiles.find { it.providerId == "openai_compatible" || it.baseUrl.contains("nvidia", ignoreCase = true) }
            if (nvidiaConfig != null && nvidiaConfig.apiKey.isNotBlank()) {
                editor.putString(KEY_NVIDIA_API_KEY, nvidiaConfig.apiKey)
            }

            // Sync active profile to legacy keys
            val active = profiles.find { it.id == activeId } ?: profiles.firstOrNull()
            if (active != null) {
                editor.putString(KEY_PROVIDER_ID, active.providerId)
                    .putString(KEY_PROVIDER_NAME, active.providerName)
                    .putString(KEY_BASE_URL, active.baseUrl)
                    .putString(KEY_CHAT_MODEL, active.chatModel)
                    .putString(KEY_IMAGE_MODEL, active.imageModel)
                    .putBoolean(KEY_IS_OPENAI_COMPATIBLE, active.isOpenAiCompatible)

                // CRITICAL: Only write to KEY_GEMINI_API_KEY if the active profile is Google Gemini!
                if (active.providerId == "gemini" && !active.apiKey.startsWith("nvapi-", ignoreCase = true)) {
                    editor.putString(KEY_GEMINI_API_KEY, active.apiKey)
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save provider profiles", e)
            false
        }
    }

    /**
     * Loads all saved provider profiles and the active profile.
     * Automatically migrates legacy single-provider storage if profiles list is empty.
     */
    suspend fun loadAllProviderProfiles(): Pair<List<ApiProviderConfig>, ApiProviderConfig> = withContext(Dispatchers.IO) {
        return@withContext try {
            val rawJson = prefs.getString(KEY_PROVIDER_PROFILES_JSON, null)
            val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)

            val profilesList = mutableListOf<ApiProviderConfig>()
            if (!rawJson.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    profilesList.add(ApiProviderConfig.fromJson(obj))
                }
            }

            if (profilesList.isEmpty()) {
                // Migrate from legacy single-provider configuration
                val legacy = loadProviderConfig()
                profilesList.add(legacy)
                saveAllProviderProfiles(profilesList, legacy.id)
                Pair(profilesList, legacy)
            } else {
                val active = profilesList.find { it.id == activeId } ?: profilesList.first()
                Pair(profilesList, active)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load provider profiles, creating default", e)
            val default = ApiProviderConfig()
            Pair(listOf(default), default)
        }
    }

    /**
     * Saves list of chat provider profiles and marks the active chat profile ID.
     */
    suspend fun saveChatProviderProfiles(
        profiles: List<ApiProviderConfig>,
        activeProfileId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonArray = org.json.JSONArray()
            profiles.forEach { profile ->
                jsonArray.put(profile.toJson())
            }
            val activeId = activeProfileId ?: profiles.firstOrNull()?.id ?: ""
            val editor = prefs.edit()
            editor.putString(KEY_CHAT_PROFILES_JSON, jsonArray.toString())
            editor.putString(KEY_ACTIVE_CHAT_PROFILE_ID, activeId)

            // Also keep legacy keys in sync
            saveAllProviderProfiles(profiles, activeId)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save chat provider profiles", e)
            false
        }
    }

    /**
     * Loads saved chat provider profiles and active chat profile.
     * Guarantees both NVIDIA NIM and Google Gemini profiles coexist with their independent keys.
     */
    suspend fun loadChatProviderProfiles(): Pair<List<ApiProviderConfig>, ApiProviderConfig> = withContext(Dispatchers.IO) {
        return@withContext try {
            val rawJson = prefs.getString(KEY_CHAT_PROFILES_JSON, null)
            val activeId = prefs.getString(KEY_ACTIVE_CHAT_PROFILE_ID, null)

            val profilesList = mutableListOf<ApiProviderConfig>()
            if (!rawJson.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    profilesList.add(ApiProviderConfig.fromJson(obj))
                }
            }

            if (profilesList.isEmpty()) {
                val (allProfiles, _) = loadAllProviderProfiles()
                if (allProfiles.isNotEmpty()) {
                    profilesList.addAll(allProfiles)
                }
            }

            val savedGoogleKey = loadGoogleApiKey() ?: ""
            val workingNvidiaKey = loadNvidiaApiKey()

            // 1. Clean profiles: ensure Gemini never carries an NVIDIA key, and has the saved Google key
            val cleanedProfiles = profilesList.map { profile ->
                if (profile.providerId == "gemini") {
                    val key = if (profile.apiKey.startsWith("nvapi-", ignoreCase = true) || profile.apiKey.isBlank()) {
                        savedGoogleKey
                    } else {
                        profile.apiKey
                    }
                    val model = if (profile.chatModel.isBlank() || profile.chatModel.contains("2.5") || profile.chatModel.contains("2.0") || profile.chatModel.contains("1.5")) {
                        "gemini-3.8-flash"
                    } else {
                        profile.chatModel
                    }
                    profile.copy(apiKey = key, chatModel = model)
                } else {
                    profile
                }
            }.toMutableList()

            // 2. Ensure Google Gemini profile is ALWAYS in the list
            var geminiProfile = cleanedProfiles.find { it.providerId == "gemini" }
            if (geminiProfile == null) {
                geminiProfile = ApiProviderConfig(
                    id = "default_chat_gemini",
                    providerId = "gemini",
                    providerName = "Google Gemini (Direct)",
                    baseUrl = "https://generativelanguage.googleapis.com",
                    apiKey = savedGoogleKey,
                    chatModel = "gemini-3.8-flash",
                    isOpenAiCompatible = false
                )
                cleanedProfiles.add(geminiProfile)
            } else if (geminiProfile.apiKey.isBlank() && savedGoogleKey.isNotBlank()) {
                val idx = cleanedProfiles.indexOf(geminiProfile)
                val updated = geminiProfile.copy(apiKey = savedGoogleKey, chatModel = "gemini-3.8-flash")
                if (idx >= 0) cleanedProfiles[idx] = updated
                geminiProfile = updated
            }

            // 3. Ensure NVIDIA NIM profile is ALWAYS in the list
            var nvidiaProfile = cleanedProfiles.find {
                it.providerId == "openai_compatible" || it.baseUrl.contains("nvidia", ignoreCase = true)
            }
            if (nvidiaProfile == null) {
                nvidiaProfile = ApiProviderConfig(
                    id = "default_chat_nvidia",
                    providerId = "openai_compatible",
                    providerName = "NVIDIA NIM",
                    baseUrl = "https://integrate.api.nvidia.com/v1",
                    apiKey = workingNvidiaKey,
                    chatModel = "meta/llama-3.2-11b-vision-instruct",
                    isOpenAiCompatible = true
                )
                cleanedProfiles.add(0, nvidiaProfile)
            } else if (nvidiaProfile.apiKey.isBlank()) {
                val idx = cleanedProfiles.indexOf(nvidiaProfile)
                val updated = nvidiaProfile.copy(apiKey = workingNvidiaKey)
                if (idx >= 0) cleanedProfiles[idx] = updated
                nvidiaProfile = updated
            }

            // 4. Select active profile:
            val targetedActive = cleanedProfiles.find { it.id == activeId }
            val active = targetedActive ?: if (nvidiaProfile.apiKey.isNotBlank()) nvidiaProfile else geminiProfile

            saveChatProviderProfiles(cleanedProfiles, active.id)
            Pair(cleanedProfiles, active)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load chat provider profiles", e)
            val default = ApiProviderConfig(
                id = "default_chat_nvidia",
                providerId = "openai_compatible",
                providerName = "NVIDIA NIM",
                baseUrl = "https://integrate.api.nvidia.com/v1",
                apiKey = "",
                chatModel = "meta/llama-3.2-11b-vision-instruct",
                isOpenAiCompatible = true
            )
            Pair(listOf(default), default)
        }
    }

    /**
     * Saves list of image provider profiles and marks the active image profile ID.
     */
    suspend fun saveImageProviderProfiles(
        profiles: List<ApiProviderConfig>,
        activeProfileId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonArray = org.json.JSONArray()
            profiles.forEach { profile ->
                jsonArray.put(profile.toJson())
            }
            val activeId = activeProfileId ?: profiles.firstOrNull()?.id ?: ""
            val editor = prefs.edit()
            editor.putString(KEY_IMAGE_PROFILES_JSON, jsonArray.toString())
            editor.putString(KEY_ACTIVE_IMAGE_PROFILE_ID, activeId)

            // Also keep dedicated keys in sync
            val geminiProfile = profiles.find { it.providerId == "gemini" }
            if (geminiProfile != null && geminiProfile.apiKey.isNotBlank() && !geminiProfile.apiKey.startsWith("nvapi-", ignoreCase = true)) {
                editor.putString(KEY_GOOGLE_API_KEY, geminiProfile.apiKey)
                editor.putString(KEY_GEMINI_API_KEY, geminiProfile.apiKey)
            }
            val nvidiaProfile = profiles.find { it.providerId == "openai_compatible" || it.baseUrl.contains("nvidia", ignoreCase = true) }
            if (nvidiaProfile != null && nvidiaProfile.apiKey.isNotBlank()) {
                editor.putString(KEY_NVIDIA_API_KEY, nvidiaProfile.apiKey)
            }

            editor.apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image provider profiles", e)
            false
        }
    }

    /**
     * Loads saved image provider profiles and active image profile.
     * Guarantees both Google Gemini (Imagen 3) and NVIDIA NIM exist with independent keys.
     */
    suspend fun loadImageProviderProfiles(): Pair<List<ApiProviderConfig>, ApiProviderConfig> = withContext(Dispatchers.IO) {
        return@withContext try {
            val rawJson = prefs.getString(KEY_IMAGE_PROFILES_JSON, null)
            val activeId = prefs.getString(KEY_ACTIVE_IMAGE_PROFILE_ID, null)

            val profilesList = mutableListOf<ApiProviderConfig>()
            if (!rawJson.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    profilesList.add(ApiProviderConfig.fromJson(obj))
                }
            }

            val savedGoogleKey = loadGoogleApiKey() ?: ""
            val workingNvidiaKey = loadNvidiaApiKey()

            // 1. Clean profiles: ensure Gemini has Google key and NVIDIA has NVIDIA key
            val cleanedImageProfiles = profilesList.map { profile ->
                if (profile.providerId == "gemini") {
                    val key = if (profile.apiKey.startsWith("nvapi-", ignoreCase = true) || profile.apiKey.isBlank()) {
                        savedGoogleKey
                    } else {
                        profile.apiKey
                    }
                    profile.copy(apiKey = key)
                } else {
                    profile
                }
            }.toMutableList()

            // 2. Ensure Google Gemini (Imagen 3) profile is always available
            var geminiImage = cleanedImageProfiles.find { it.providerId == "gemini" }
            if (geminiImage == null) {
                geminiImage = ApiProviderConfig(
                    id = "default_image_gemini",
                    providerId = "gemini",
                    providerName = "Google Gemini (Direct)",
                    baseUrl = "https://generativelanguage.googleapis.com",
                    apiKey = savedGoogleKey,
                    imageModel = "imagen-3.0-generate-002",
                    isOpenAiCompatible = false
                )
                cleanedImageProfiles.add(0, geminiImage)
            } else if (geminiImage.apiKey.isBlank() && savedGoogleKey.isNotBlank()) {
                val idx = cleanedImageProfiles.indexOf(geminiImage)
                val updated = geminiImage.copy(apiKey = savedGoogleKey)
                if (idx >= 0) cleanedImageProfiles[idx] = updated
                geminiImage = updated
            }

            // 3. Ensure NVIDIA image profile also exists
            var nvidiaImage = cleanedImageProfiles.find {
                it.providerId == "openai_compatible" || it.baseUrl.contains("nvidia", ignoreCase = true)
            }
            if (nvidiaImage == null) {
                nvidiaImage = ApiProviderConfig(
                    id = "default_image_nvidia",
                    providerId = "openai_compatible",
                    providerName = "NVIDIA NIM (Images)",
                    baseUrl = "https://integrate.api.nvidia.com/v1",
                    apiKey = workingNvidiaKey,
                    imageModel = "black-forest-labs/flux.1-dev",
                    isOpenAiCompatible = true
                )
                cleanedImageProfiles.add(nvidiaImage)
            } else {
                var updated = nvidiaImage
                if (nvidiaImage.apiKey.isBlank()) {
                    updated = updated.copy(apiKey = workingNvidiaKey)
                }
                if (updated.imageModel == "qwen-image" || updated.imageModel.isBlank()) {
                    updated = updated.copy(imageModel = "black-forest-labs/flux.1-dev")
                }
                val idx = cleanedImageProfiles.indexOf(nvidiaImage)
                if (idx >= 0) cleanedImageProfiles[idx] = updated
                nvidiaImage = updated
            }

            val targetedActive = cleanedImageProfiles.find { it.id == activeId }
            val active = targetedActive ?: geminiImage
            saveImageProviderProfiles(cleanedImageProfiles, active.id)
            Pair(cleanedImageProfiles, active)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image provider profiles", e)
            val default = ApiProviderConfig(
                id = "default_image_gemini",
                providerId = "gemini",
                providerName = "Google Gemini (Direct)",
                imageModel = "imagen-3.0-generate-002"
            )
            Pair(listOf(default), default)
        }
    }

    companion object {
        private const val TAG = "SecureStorageManager"
        private const val PREFS_FILE_NAME = "bannercraft_secure_prefs"
        private const val PREFS_FALLBACK_FILE = "bannercraft_fallback_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GOOGLE_API_KEY = "google_gemini_api_key_dedicated"
        private const val KEY_NVIDIA_API_KEY = "nvidia_nim_api_key_dedicated"
        private const val KEY_SELECTED_MODEL = "gemini_selected_model"

        private const val KEY_PROVIDER_ID = "api_provider_id"
        private const val KEY_PROVIDER_NAME = "api_provider_name"
        private const val KEY_BASE_URL = "api_base_url"
        private const val KEY_CHAT_MODEL = "api_chat_model"
        private const val KEY_IMAGE_MODEL = "api_image_model"
        private const val KEY_IS_OPENAI_COMPATIBLE = "api_is_openai_compatible"

        private const val KEY_PROVIDER_PROFILES_JSON = "api_provider_profiles_json"
        private const val KEY_ACTIVE_PROFILE_ID = "api_active_profile_id"

        private const val KEY_CHAT_PROFILES_JSON = "chat_provider_profiles_json"
        private const val KEY_ACTIVE_CHAT_PROFILE_ID = "chat_active_profile_id"
        private const val KEY_IMAGE_PROFILES_JSON = "image_provider_profiles_json"
        private const val KEY_ACTIVE_IMAGE_PROFILE_ID = "image_active_profile_id"
    }
}
