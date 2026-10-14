package com.example.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiProviderConfig
import com.example.data.ChatMessage
import com.example.data.GeminiChatService
import com.example.data.OpenAiCompatibleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PromptChatState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            isUser = false,
            text = "Hey Creator! 👋 I'm your AI Creative Director. Tell me about your YouTube channel (your niche, favorite colors, vibe, or channel name), and I'll craft creative banner concepts and ready-to-use visual prompts for you!"
        )
    ),
    val isTyping: Boolean = false,
    val errorMessage: String? = null
)

class PromptChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(PromptChatState())
    val state: StateFlow<PromptChatState> = _state.asStateFlow()

    private val geminiChatService = GeminiChatService()
    private val openAiService = OpenAiCompatibleService()

    fun sendMessage(
        userText: String,
        providerConfig: ApiProviderConfig? = null,
        apiKey: String? = null,
        model: String? = null
    ) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _state.value.isTyping) return

        val effectiveConfig = providerConfig ?: ApiProviderConfig(
            apiKey = apiKey.orEmpty(),
            chatModel = model ?: "gemini-2.0-flash",
            isOpenAiCompatible = false
        )

        val effectiveKey = effectiveConfig.apiKey.ifBlank { apiKey.orEmpty() }

        if (effectiveKey.isBlank() && effectiveConfig.providerId != "ollama") {
            _state.update {
                it.copy(errorMessage = "Please configure your API Key in Settings or Provider Setup first.")
            }
            return
        }

        val userMessage = ChatMessage(isUser = true, text = trimmed)
        val updatedMessages = _state.value.messages + userMessage

        // Auto-heal / intelligent provider routing:
        // Automatically routes to matching provider if key format unambiguously indicates the target provider
        val isNvidiaKey = effectiveKey.startsWith("nvapi-", ignoreCase = true)
        val isGoogleKey = effectiveKey.startsWith("AIzaSy")
        val resolvedConfig = if (!effectiveConfig.isOpenAiCompatible && isNvidiaKey) {
            effectiveConfig.copy(
                providerId = "openai_compatible",
                providerName = "NVIDIA NIM",
                baseUrl = "https://integrate.api.nvidia.com/v1",
                chatModel = "meta/llama-3.2-11b-vision-instruct",
                isOpenAiCompatible = true
            )
        } else if (effectiveConfig.isOpenAiCompatible && isGoogleKey) {
            effectiveConfig.copy(
                providerId = "gemini",
                providerName = "Google Gemini (Direct)",
                baseUrl = "https://generativelanguage.googleapis.com",
                chatModel = "gemini-3.8-flash",
                isOpenAiCompatible = false
            )
        } else {
            effectiveConfig
        }

        _state.update {
            it.copy(
                messages = updatedMessages,
                isTyping = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val assistantText = if (resolvedConfig.isOpenAiCompatible) {
                    openAiService.sendChat(
                        baseUrl = resolvedConfig.baseUrl,
                        apiKey = effectiveKey,
                        model = resolvedConfig.chatModel,
                        messages = updatedMessages
                    )
                } else {
                    geminiChatService.sendChat(
                        apiKey = effectiveKey,
                        messages = updatedMessages,
                        model = resolvedConfig.chatModel
                    )
                }

                val assistantMessage = ChatMessage(isUser = false, text = assistantText)
                _state.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isTyping = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                val rawMsg = e.message ?: "Failed to generate AI response. Please try again."
                val userFriendlyMsg = when {
                    rawMsg.contains("SAFETY", ignoreCase = true) ||
                    rawMsg.contains("PROHIBITED_CONTENT", ignoreCase = true) ||
                    rawMsg.contains("content_filter", ignoreCase = true) ||
                    rawMsg.contains("safety policy", ignoreCase = true) -> {
                        "⚠️ Content Moderated: This message was flagged by AI safety guidelines. Please modify your text and try again."
                    }
                    rawMsg.contains("API_KEY_SERVICE_BLOCKED", ignoreCase = true) || rawMsg.contains("Requests to this API generativelanguage.googleapis.com method", ignoreCase = true) -> {
                        "This Google API key does not have Generative Language API access enabled. Please create a free Gemini API key from Google AI Studio (aistudio.google.com) or switch to NVIDIA NIM!"
                    }
                    rawMsg.contains("API key not valid", ignoreCase = true) ||
                    rawMsg.contains("status 400", ignoreCase = true) ||
                    rawMsg.contains("INVALID_ARGUMENT", ignoreCase = true) -> {
                        "Google Gemini rejected the API key (Status 400). Please enter a valid Google AI Studio key (AIzaSy...), or switch to NVIDIA NIM!"
                    }
                    else -> rawMsg
                }
                _state.update {
                    it.copy(
                        isTyping = false,
                        errorMessage = userFriendlyMsg
                    )
                }
            }
        }
    }

    fun clearChat() {
        _state.update {
            PromptChatState()
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
