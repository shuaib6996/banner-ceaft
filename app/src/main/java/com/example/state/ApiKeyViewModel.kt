package com.example.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiModelConfig
import com.example.data.ApiProviderConfig
import com.example.data.ApiProviderPresets
import com.example.data.ConnectionTestResult
import com.example.data.GeminiChatService
import com.example.data.OpenAiCompatibleService
import com.example.data.ProviderPreset
import com.example.data.SecureStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ConfigTab { CHAT, IMAGE }

data class ApiKeyState(
    val chatConfig: ApiProviderConfig = ApiProviderConfig(
        id = "default_chat_nvidia",
        providerId = "openai_compatible",
        providerName = "NVIDIA NIM",
        baseUrl = "https://integrate.api.nvidia.com/v1",
        apiKey = "",
        chatModel = "meta/llama-3.2-11b-vision-instruct",
        isOpenAiCompatible = true
    ),
    val imageConfig: ApiProviderConfig = ApiProviderConfig(
        id = "default_image_nvidia",
        providerId = "openai_compatible",
        providerName = "NVIDIA NIM (Images)",
        baseUrl = "https://integrate.api.nvidia.com/v1",
        apiKey = "",
        imageModel = "black-forest-labs/flux.1-dev",
        isOpenAiCompatible = true
    ),
    val savedChatProfiles: List<ApiProviderConfig> = emptyList(),
    val savedImageProfiles: List<ApiProviderConfig> = emptyList(),
    val activeTab: ConfigTab = ConfigTab.CHAT,
    val isKeySaved: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isTestingConnection: Boolean = false,
    val testConnectionResult: ConnectionTestResult? = null
) {
    // Current editing configuration based on active tab
    val currentEditingConfig: ApiProviderConfig get() = if (activeTab == ConfigTab.CHAT) chatConfig else imageConfig
    val currentSavedProfiles: List<ApiProviderConfig> get() = if (activeTab == ConfigTab.CHAT) savedChatProfiles else savedImageProfiles

    // Backward compatibility delegates
    val providerConfig: ApiProviderConfig get() = currentEditingConfig
    val savedProfiles: List<ApiProviderConfig> get() = currentSavedProfiles
    val apiKey: String? get() = (if (activeTab == ConfigTab.CHAT) chatConfig.apiKey else imageConfig.apiKey).ifBlank { null }
    val selectedModel: String get() = chatConfig.chatModel
    val imageModel: String get() = imageConfig.imageModel
}

sealed class ApiKeyValidationResult {
    object Valid : ApiKeyValidationResult()
    data class Invalid(val errorMessage: String) : ApiKeyValidationResult()
}

class ApiKeyViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = SecureStorageManager(application)
    private val openAiService = OpenAiCompatibleService()
    private val geminiChatService = GeminiChatService()

    private val _state = MutableStateFlow(ApiKeyState())
    val state: StateFlow<ApiKeyState> = _state.asStateFlow()

    init {
        loadKey()
    }

    /**
     * Sanitizes API key input by stripping hidden spaces, newlines, tabs, quotes,
     * accidental prefixes (e.g. 'api key.', 'bearer'), and accidental leading/trailing punctuation.
     */
    fun sanitizeKey(key: String): String {
        var cleaned = key.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .replace("\\s+".toRegex(), "")

        // Remove accidental user prefixes like "api key:", "api key.", "apikey:", "Bearer"
        cleaned = cleaned.replace("(?i)^(api[-_]?key[:.]?|bearer)+".toRegex(), "")

        // Strip accidental leading and trailing dots, colons, or punctuation
        return cleaned.trimStart('.', ':', ';', ',', '-', ' ').trimEnd('.', ':', ';', ',', ' ')
    }

    /**
     * Flexible validator allowing API keys or blank for local Ollama.
     */
    fun validateApiKey(rawKey: String, allowEmpty: Boolean = false): ApiKeyValidationResult {
        val cleaned = sanitizeKey(rawKey)
        if (cleaned.isEmpty() && !allowEmpty) {
            return ApiKeyValidationResult.Invalid("API key cannot be empty.")
        }
        if (cleaned.isNotEmpty() && cleaned.length < 5) {
            return ApiKeyValidationResult.Invalid("API key is too short.")
        }
        return ApiKeyValidationResult.Valid
    }

    fun isValidFormat(key: String): Boolean {
        return validateApiKey(key) is ApiKeyValidationResult.Valid
    }

    fun setActiveTab(tab: ConfigTab) {
        _state.update {
            it.copy(
                activeTab = tab,
                errorMessage = null,
                testConnectionResult = null
            )
        }
    }

    /**
     * Starts creating a new profile with a fresh ID based on a preset.
     */
    fun startNewProfile(preset: ProviderPreset = ApiProviderPresets.PRESET_OPENAI_COMPATIBLE) {
        val isChat = _state.value.activeTab == ConfigTab.CHAT
        val newConfig = ApiProviderConfig(
            id = UUID.randomUUID().toString(),
            providerId = preset.id,
            providerName = preset.name,
            baseUrl = preset.baseUrl,
            apiKey = "",
            chatModel = preset.defaultChatModel,
            imageModel = preset.defaultImageModel,
            isOpenAiCompatible = preset.isOpenAiCompatible
        )

        _state.update {
            if (isChat) {
                it.copy(
                    chatConfig = newConfig,
                    errorMessage = null,
                    testConnectionResult = null
                )
            } else {
                it.copy(
                    imageConfig = newConfig,
                    errorMessage = null,
                    testConnectionResult = null
                )
            }
        }
    }

    /**
     * Selects an existing saved profile or switches editing target to it.
     */
    fun selectProfileToEdit(profile: ApiProviderConfig) {
        _state.update {
            if (it.activeTab == ConfigTab.CHAT) {
                it.copy(
                    chatConfig = profile,
                    errorMessage = null,
                    testConnectionResult = null
                )
            } else {
                it.copy(
                    imageConfig = profile,
                    errorMessage = null,
                    testConnectionResult = null
                )
            }
        }
    }

    /**
     * Switches the active chat provider profile by ID.
     */
    fun switchActiveChatProfile(profileId: String) {
        viewModelScope.launch {
            val profiles = _state.value.savedChatProfiles
            val target = profiles.find { it.id == profileId } ?: return@launch
            val isSaved = target.apiKey.isNotBlank() || target.providerId == "ollama"

            _state.update {
                it.copy(
                    chatConfig = target,
                    isKeySaved = isSaved,
                    errorMessage = null,
                    testConnectionResult = null
                )
            }
            storageManager.saveChatProviderProfiles(profiles, target.id)
        }
    }

    /**
     * Instantly switches active chat profile to NVIDIA NIM (tested working provider).
     */
    fun switchToNvidiaChat() {
        viewModelScope.launch {
            val profiles = _state.value.savedChatProfiles
            var target = profiles.find {
                it.providerId == "openai_compatible" || it.baseUrl.contains("nvidia", ignoreCase = true)
            }
            if (target == null) {
                val savedNvidiaKey = storageManager.loadNvidiaApiKey()
                target = ApiProviderConfig(
                    id = "default_chat_nvidia",
                    providerId = "openai_compatible",
                    providerName = "NVIDIA NIM",
                    baseUrl = "https://integrate.api.nvidia.com/v1",
                    apiKey = savedNvidiaKey,
                    chatModel = "meta/llama-3.2-11b-vision-instruct",
                    isOpenAiCompatible = true
                )
                val updatedProfiles = listOf(target) + profiles
                _state.update {
                    it.copy(
                        chatConfig = target,
                        savedChatProfiles = updatedProfiles,
                        isKeySaved = true,
                        errorMessage = null,
                        testConnectionResult = null
                    )
                }
                storageManager.saveChatProviderProfiles(updatedProfiles, target.id)
            } else {
                switchActiveChatProfile(target.id)
            }
        }
    }

    /**
     * Instantly switches active chat profile to Google Gemini.
     */
    fun switchToGoogleChat() {
        viewModelScope.launch {
            val profiles = _state.value.savedChatProfiles
            var target = profiles.find { it.providerId == "gemini" }
            if (target != null) {
                val updated = if (target.chatModel.isBlank() || target.chatModel.contains("2.5") || target.chatModel.contains("2.0") || target.chatModel.contains("1.5")) {
                    target.copy(chatModel = "gemini-3.8-flash")
                } else target
                val updatedProfiles = profiles.map { if (it.id == target.id) updated else it }
                _state.update {
                    it.copy(
                        chatConfig = updated,
                        savedChatProfiles = updatedProfiles,
                        isKeySaved = updated.apiKey.isNotBlank(),
                        errorMessage = null,
                        testConnectionResult = null
                    )
                }
                storageManager.saveChatProviderProfiles(updatedProfiles, updated.id)
            } else {
                val googleKey = storageManager.loadGoogleApiKey().orEmpty()
                target = ApiProviderConfig(
                    id = "default_chat_gemini",
                    providerId = "gemini",
                    providerName = "Google Gemini (Direct)",
                    baseUrl = "https://generativelanguage.googleapis.com",
                    apiKey = googleKey,
                    chatModel = "gemini-3.8-flash",
                    isOpenAiCompatible = false
                )
                val updatedProfiles = profiles + target
                _state.update {
                    it.copy(
                        chatConfig = target,
                        savedChatProfiles = updatedProfiles,
                        isKeySaved = googleKey.isNotBlank(),
                        errorMessage = null,
                        testConnectionResult = null
                    )
                }
                storageManager.saveChatProviderProfiles(updatedProfiles, target.id)
            }
        }
    }

    /**
     * Switches the active image provider profile by ID.
     */
    fun switchActiveImageProfile(profileId: String) {
        viewModelScope.launch {
            val profiles = _state.value.savedImageProfiles
            val target = profiles.find { it.id == profileId } ?: return@launch

            _state.update {
                it.copy(
                    imageConfig = target,
                    errorMessage = null,
                    testConnectionResult = null
                )
            }
            storageManager.saveImageProviderProfiles(profiles, target.id)
        }
    }

    /**
     * Switches active profile for the currently selected tab (backward compatible).
     */
    fun switchActiveProfile(profileId: String) {
        if (_state.value.activeTab == ConfigTab.CHAT) {
            switchActiveChatProfile(profileId)
        } else {
            switchActiveImageProfile(profileId)
        }
    }

    /**
     * Instant in-chat model switcher: updates chat model on the active chat profile and saves.
     */
    fun switchChatModel(modelId: String) {
        viewModelScope.launch {
            val currentActive = _state.value.chatConfig.copy(chatModel = modelId)
            val updatedProfiles = _state.value.savedChatProfiles.map {
                if (it.id == currentActive.id) currentActive else it
            }
            _state.update {
                it.copy(
                    chatConfig = currentActive,
                    savedChatProfiles = updatedProfiles
                )
            }
            storageManager.saveChatProviderProfiles(updatedProfiles, currentActive.id)
            storageManager.saveSelectedModel(modelId)
        }
    }

    /**
     * Instant in-studio image model switcher: updates image model on the active image profile and saves.
     */
    fun switchImageModel(modelId: String) {
        viewModelScope.launch {
            val currentActive = _state.value.imageConfig.copy(imageModel = modelId)
            val updatedProfiles = _state.value.savedImageProfiles.map {
                if (it.id == currentActive.id) currentActive else it
            }
            _state.update {
                it.copy(
                    imageConfig = currentActive,
                    savedImageProfiles = updatedProfiles
                )
            }
            storageManager.saveImageProviderProfiles(updatedProfiles, currentActive.id)
        }
    }

    /**
     * Instantly switches active image profile to NVIDIA NIM (FLUX.1 Dev).
     */
    fun switchToNvidiaImage() {
        viewModelScope.launch {
            val profiles = _state.value.savedImageProfiles
            var target = profiles.find {
                it.providerId == "openai_compatible" || it.baseUrl.contains("nvidia", ignoreCase = true)
            }
            if (target == null) {
                val savedNvidiaKey = storageManager.loadNvidiaApiKey()
                target = ApiProviderConfig(
                    id = "default_image_nvidia",
                    providerId = "openai_compatible",
                    providerName = "NVIDIA NIM (Images)",
                    baseUrl = "https://integrate.api.nvidia.com/v1",
                    apiKey = savedNvidiaKey,
                    imageModel = "black-forest-labs/flux.1-dev",
                    isOpenAiCompatible = true
                )
                val updatedProfiles = listOf(target) + profiles
                _state.update {
                    it.copy(
                        imageConfig = target,
                        savedImageProfiles = updatedProfiles
                    )
                }
                storageManager.saveImageProviderProfiles(updatedProfiles, target.id)
            } else {
                switchActiveImageProfile(target.id)
            }
        }
    }

    /**
     * Instantly switches active image profile to Google Gemini (Imagen 3).
     */
    fun switchToGoogleImage() {
        viewModelScope.launch {
            val profiles = _state.value.savedImageProfiles
            var target = profiles.find { it.providerId == "gemini" }
            if (target != null) {
                switchActiveImageProfile(target.id)
            } else {
                val googleKey = storageManager.loadGoogleApiKey().orEmpty()
                target = ApiProviderConfig(
                    id = "default_image_gemini",
                    providerId = "gemini",
                    providerName = "Google Gemini (Direct)",
                    baseUrl = "https://generativelanguage.googleapis.com",
                    apiKey = googleKey,
                    imageModel = "imagen-3.0-generate-002",
                    isOpenAiCompatible = false
                )
                val updatedProfiles = listOf(target) + profiles
                _state.update {
                    it.copy(
                        imageConfig = target,
                        savedImageProfiles = updatedProfiles
                    )
                }
                storageManager.saveImageProviderProfiles(updatedProfiles, target.id)
            }
        }
    }

    /**
     * Selects a provider preset for the currently active editing profile.
     * Switches to existing profile if one exists for this provider to prevent accidental overwrites.
     */
    fun selectPreset(preset: ProviderPreset) {
        viewModelScope.launch {
            val isChat = _state.value.activeTab == ConfigTab.CHAT
            val currentProfiles = if (isChat) _state.value.savedChatProfiles else _state.value.savedImageProfiles

            // Check if user already has a saved profile matching this preset
            val existing = currentProfiles.find {
                if (preset.id == "gemini") {
                    it.providerId == "gemini"
                } else if (preset.id == "openai_compatible") {
                    it.providerId == "openai_compatible" || it.baseUrl.contains("nvidia", ignoreCase = true)
                } else {
                    it.providerId == preset.id
                }
            }

            if (existing != null) {
                // Switch directly to the existing profile! This preserves its ID, key, and models
                selectProfileToEdit(existing)
                return@launch
            }

            // If not found, load dedicated default key for this preset
            val defaultKey = when (preset.id) {
                "gemini" -> storageManager.loadGoogleApiKey().orEmpty()
                "openai_compatible" -> storageManager.loadNvidiaApiKey()
                else -> ""
            }

            val newConfig = ApiProviderConfig(
                id = UUID.randomUUID().toString(),
                providerId = preset.id,
                providerName = preset.name,
                baseUrl = preset.baseUrl,
                apiKey = defaultKey,
                chatModel = preset.defaultChatModel,
                imageModel = preset.defaultImageModel,
                isOpenAiCompatible = preset.isOpenAiCompatible
            )

            _state.update { current ->
                if (isChat) {
                    current.copy(
                        chatConfig = newConfig,
                        testConnectionResult = null,
                        errorMessage = null
                    )
                } else {
                    current.copy(
                        imageConfig = newConfig,
                        testConnectionResult = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    /**
     * Updates individual provider properties while editing.
     */
    fun updateProviderDetails(
        providerId: String? = null,
        providerName: String? = null,
        baseUrl: String? = null,
        apiKey: String? = null,
        chatModel: String? = null,
        imageModel: String? = null,
        isOpenAiCompatible: Boolean? = null
    ) {
        _state.update { current ->
            if (current.activeTab == ConfigTab.CHAT) {
                val cfg = current.chatConfig
                current.copy(
                    chatConfig = cfg.copy(
                        providerId = providerId ?: cfg.providerId,
                        providerName = providerName ?: cfg.providerName,
                        baseUrl = baseUrl ?: cfg.baseUrl,
                        apiKey = apiKey ?: cfg.apiKey,
                        chatModel = chatModel ?: cfg.chatModel,
                        imageModel = imageModel ?: cfg.imageModel,
                        isOpenAiCompatible = isOpenAiCompatible ?: cfg.isOpenAiCompatible
                    ),
                    errorMessage = null
                )
            } else {
                val cfg = current.imageConfig
                current.copy(
                    imageConfig = cfg.copy(
                        providerId = providerId ?: cfg.providerId,
                        providerName = providerName ?: cfg.providerName,
                        baseUrl = baseUrl ?: cfg.baseUrl,
                        apiKey = apiKey ?: cfg.apiKey,
                        chatModel = chatModel ?: cfg.chatModel,
                        imageModel = imageModel ?: cfg.imageModel,
                        isOpenAiCompatible = isOpenAiCompatible ?: cfg.isOpenAiCompatible
                    ),
                    errorMessage = null
                )
            }
        }
    }

    /**
     * 1-tap utility to copy the active Chat API key into the Image Provider configuration.
     */
    fun copyChatKeyToImage() {
        val chatKey = _state.value.chatConfig.apiKey
        _state.update {
            it.copy(
                imageConfig = it.imageConfig.copy(apiKey = chatKey),
                errorMessage = null
            )
        }
    }

    /**
     * Tests connectivity to the OpenAI-compatible endpoint with current URL, key, and model.
     */
    fun testConnection(
        baseUrl: String = _state.value.currentEditingConfig.baseUrl,
        apiKey: String = _state.value.currentEditingConfig.apiKey,
        model: String = if (_state.value.activeTab == ConfigTab.CHAT) _state.value.chatConfig.chatModel else _state.value.imageConfig.imageModel
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isTestingConnection = true, testConnectionResult = null) }
            val isGemini = baseUrl.contains("generativelanguage.googleapis.com", ignoreCase = true) ||
                _state.value.currentEditingConfig.providerId == "gemini"
            val result = if (isGemini) {
                geminiChatService.testConnection(
                    apiKey = apiKey,
                    model = model
                )
            } else {
                openAiService.testConnection(
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model
                )
            }
            _state.update {
                it.copy(
                    isTestingConnection = false,
                    testConnectionResult = result
                )
            }
        }
    }

    /**
     * Saves or updates a provider configuration in the list of saved profiles.
     */
    fun saveProviderConfig(
        config: ApiProviderConfig = _state.value.currentEditingConfig,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sanitizedKey = sanitizeKey(config.apiKey)
        val allowBlankKey = config.providerId == "ollama"

        if (!allowBlankKey && sanitizedKey.isBlank()) {
            onError("Please enter a valid API Key")
            return
        }

        // Prevent cross-contamination
        if (config.providerId == "gemini" && sanitizedKey.startsWith("nvapi-", ignoreCase = true)) {
            onError("This is an NVIDIA NIM API key (nvapi-...). For Google Gemini, please enter a Google AI Studio key (AIzaSy...). To use NVIDIA NIM, switch to the OpenAI Compatible preset!")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val cleanConfig = config.copy(
                apiKey = sanitizedKey,
                baseUrl = config.baseUrl.trim(),
                chatModel = config.chatModel.trim(),
                imageModel = config.imageModel.trim()
            )

            val isChat = _state.value.activeTab == ConfigTab.CHAT

            // Dedicated storage sync based on provider type:
            if (cleanConfig.providerId == "gemini") {
                storageManager.saveGoogleApiKey(sanitizedKey)
            } else if (cleanConfig.providerId == "openai_compatible" || cleanConfig.baseUrl.contains("nvidia", ignoreCase = true)) {
                storageManager.saveNvidiaApiKey(sanitizedKey)
            }

            if (isChat) {
                val currentProfiles = _state.value.savedChatProfiles.toMutableList()
                val existingIndex = currentProfiles.indexOfFirst { it.id == cleanConfig.id }
                if (existingIndex >= 0) {
                    currentProfiles[existingIndex] = cleanConfig
                } else {
                    currentProfiles.add(cleanConfig)
                }

                // Also sync key to matching image profile if available
                val currentImageProfiles = _state.value.savedImageProfiles.toMutableList()
                val matchingImageIdx = currentImageProfiles.indexOfFirst { it.providerId == cleanConfig.providerId }
                if (matchingImageIdx >= 0) {
                    val img = currentImageProfiles[matchingImageIdx]
                    currentImageProfiles[matchingImageIdx] = img.copy(apiKey = sanitizedKey)
                    storageManager.saveImageProviderProfiles(currentImageProfiles)
                }

                storageManager.saveChatProviderProfiles(currentProfiles, cleanConfig.id)
                storageManager.saveSelectedModel(cleanConfig.chatModel)

                _state.update {
                    it.copy(
                        chatConfig = cleanConfig,
                        savedChatProfiles = currentProfiles,
                        savedImageProfiles = currentImageProfiles,
                        isKeySaved = true,
                        isLoading = false,
                        testConnectionResult = null
                    )
                }
            } else {
                val currentProfiles = _state.value.savedImageProfiles.toMutableList()
                val existingIndex = currentProfiles.indexOfFirst { it.id == cleanConfig.id }
                if (existingIndex >= 0) {
                    currentProfiles[existingIndex] = cleanConfig
                } else {
                    currentProfiles.add(cleanConfig)
                }

                // Also sync key to matching chat profile if available
                val currentChatProfiles = _state.value.savedChatProfiles.toMutableList()
                val matchingChatIdx = currentChatProfiles.indexOfFirst { it.providerId == cleanConfig.providerId }
                if (matchingChatIdx >= 0) {
                    val chat = currentChatProfiles[matchingChatIdx]
                    currentChatProfiles[matchingChatIdx] = chat.copy(apiKey = sanitizedKey)
                    storageManager.saveChatProviderProfiles(currentChatProfiles)
                }

                storageManager.saveImageProviderProfiles(currentProfiles, cleanConfig.id)

                _state.update {
                    it.copy(
                        imageConfig = cleanConfig,
                        savedImageProfiles = currentProfiles,
                        savedChatProfiles = currentChatProfiles,
                        isKeySaved = true,
                        isLoading = false,
                        testConnectionResult = null
                    )
                }
            }
            onSuccess()
        }
    }

    /**
     * Deletes a provider profile by ID.
     */
    fun deleteProfile(profileId: String, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            val isChat = _state.value.activeTab == ConfigTab.CHAT
            if (isChat) {
                val remaining = _state.value.savedChatProfiles.filter { it.id != profileId }.toMutableList()
                val newActive = if (remaining.isNotEmpty()) {
                    remaining.first()
                } else {
                    val fallback = ApiProviderConfig()
                    remaining.add(fallback)
                    fallback
                }
                storageManager.saveChatProviderProfiles(remaining, newActive.id)
                _state.update {
                    it.copy(
                        chatConfig = newActive,
                        savedChatProfiles = remaining,
                        isKeySaved = newActive.apiKey.isNotBlank() || newActive.providerId == "ollama"
                    )
                }
            } else {
                val remaining = _state.value.savedImageProfiles.filter { it.id != profileId }.toMutableList()
                val newActive = if (remaining.isNotEmpty()) {
                    remaining.first()
                } else {
                    val fallback = ApiProviderConfig(
                        providerId = "gemini",
                        providerName = "Google Gemini (Direct)",
                        imageModel = "imagen-3.0-generate-002"
                    )
                    remaining.add(fallback)
                    fallback
                }
                storageManager.saveImageProviderProfiles(remaining, newActive.id)
                _state.update {
                    it.copy(
                        imageConfig = newActive,
                        savedImageProfiles = remaining
                    )
                }
            }
            onDeleted?.invoke()
        }
    }

    /**
     * Backward-compatible key saver.
     */
    fun saveKey(key: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentConfig = _state.value.currentEditingConfig.copy(apiKey = key)
        saveProviderConfig(currentConfig, onSuccess, onError)
    }

    fun setSelectedModel(modelId: String) {
        switchChatModel(modelId)
    }

    fun loadKey(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val (chatProfiles, activeChat) = storageManager.loadChatProviderProfiles()
            val (imageProfiles, activeImage) = storageManager.loadImageProviderProfiles()

            val saved = activeChat.apiKey.isNotBlank() || activeImage.apiKey.isNotBlank()

            _state.update {
                it.copy(
                    chatConfig = activeChat,
                    savedChatProfiles = chatProfiles,
                    imageConfig = activeImage,
                    savedImageProfiles = imageProfiles,
                    isKeySaved = saved,
                    isLoading = false
                )
            }
            onComplete?.invoke(saved)
        }
    }

    fun clearKey(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            storageManager.clearApiKey()
            val resetChat = _state.value.chatConfig.copy(apiKey = "")
            val resetImage = _state.value.imageConfig.copy(apiKey = "")
            storageManager.saveChatProviderProfiles(listOf(resetChat), resetChat.id)
            storageManager.saveImageProviderProfiles(listOf(resetImage), resetImage.id)
            _state.update {
                it.copy(
                    chatConfig = resetChat,
                    imageConfig = resetImage,
                    savedChatProfiles = listOf(resetChat),
                    savedImageProfiles = listOf(resetImage),
                    isKeySaved = false,
                    isLoading = false,
                    testConnectionResult = null
                )
            }
            onComplete()
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearTestResult() {
        _state.update { it.copy(testConnectionResult = null) }
    }
}
