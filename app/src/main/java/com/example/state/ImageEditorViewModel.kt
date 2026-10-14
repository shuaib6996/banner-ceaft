package com.example.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiImageService
import com.example.data.InvalidApiKeyException
import com.example.data.NoImageGeneratedException
import com.example.data.RateLimitException
import com.example.data.SaveResult
import com.example.data.SaveService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class ImageEditorState(
    val prompt: String = "",
    val editInstruction: String = "",
    val mode: ImageEditorMode = ImageEditorMode.CREATE, // CREATE vs EDIT
    val selectedAspectRatio: String = "16:9",
    val sourceImageBytes: ByteArray? = null,
    val resultImageBytes: ByteArray? = null,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val stageMessage: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageEditorState

        if (prompt != other.prompt) return false
        if (editInstruction != other.editInstruction) return false
        if (mode != other.mode) return false
        if (selectedAspectRatio != other.selectedAspectRatio) return false
        if (isProcessing != other.isProcessing) return false
        if (isSaving != other.isSaving) return false
        if (stageMessage != other.stageMessage) return false
        if (errorMessage != other.errorMessage) return false
        if (successMessage != other.successMessage) return false

        if (sourceImageBytes != null) {
            if (other.sourceImageBytes == null) return false
            if (!sourceImageBytes.contentEquals(other.sourceImageBytes)) return false
        } else if (other.sourceImageBytes != null) return false

        if (resultImageBytes != null) {
            if (other.resultImageBytes == null) return false
            if (!resultImageBytes.contentEquals(other.resultImageBytes)) return false
        } else if (other.resultImageBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prompt.hashCode()
        result = 31 * result + editInstruction.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + selectedAspectRatio.hashCode()
        result = 31 * result + (sourceImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (resultImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + isProcessing.hashCode()
        result = 31 * result + isSaving.hashCode()
        result = 31 * result + (stageMessage?.hashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (successMessage?.hashCode() ?: 0)
        return result
    }
}

enum class ImageEditorMode {
    CREATE,
    EDIT
}

class ImageEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(ImageEditorState())
    val state: StateFlow<ImageEditorState> = _state.asStateFlow()

    private val geminiService = GeminiImageService()
    private val openAiService = com.example.data.OpenAiCompatibleService()

    fun updateMode(mode: ImageEditorMode) {
        _state.update { it.copy(mode = mode, errorMessage = null, successMessage = null) }
    }

    fun updatePrompt(prompt: String) {
        _state.update { it.copy(prompt = prompt, errorMessage = null) }
    }

    fun updateEditInstruction(instruction: String) {
        _state.update { it.copy(editInstruction = instruction, errorMessage = null) }
    }

    fun updateAspectRatio(ratio: String) {
        _state.update { it.copy(selectedAspectRatio = ratio) }
    }

    fun setSourceImage(bytes: ByteArray?) {
        _state.update {
            it.copy(
                sourceImageBytes = bytes,
                mode = if (bytes != null) ImageEditorMode.EDIT else it.mode,
                errorMessage = null
            )
        }
    }

    fun processImage(
        apiKey: String?,
        model: String = com.example.data.AiModelConfig.DEFAULT_MODEL,
        providerConfig: com.example.data.ApiProviderConfig? = null,
        onSaveToHistory: ((title: String, prompt: String, bytes: ByteArray) -> Unit)? = null
    ) {
        if (_state.value.isProcessing) return

        val effectiveConfig = providerConfig ?: com.example.data.ApiProviderConfig(
            apiKey = apiKey.orEmpty(),
            imageModel = model,
            isOpenAiCompatible = false
        )

        val effectiveKey = effectiveConfig.apiKey.ifBlank { apiKey.orEmpty() }

        if (effectiveKey.isBlank() && effectiveConfig.providerId != "ollama") {
            _state.update {
                it.copy(
                    errorMessage = "API key is required for ${effectiveConfig.providerName}. Please add it in Settings."
                )
            }
            return
        }

        val targetModel = effectiveConfig.imageModel.ifBlank { model }
        val isNvidiaKey = effectiveKey.startsWith("nvapi-", ignoreCase = true)
        val isGoogleKey = effectiveKey.startsWith("AIzaSy")
        val isOpenAi = isNvidiaKey || (effectiveConfig.isOpenAiCompatible && !isGoogleKey)

        // Set isProcessing immediately on caller thread to prevent double-click race conditions
        val current = _state.value
        _state.update {
            it.copy(
                isProcessing = true,
                errorMessage = null,
                successMessage = null,
                stageMessage = if (current.mode == ImageEditorMode.CREATE) "Creating image with $targetModel..." else "Editing image with prompt..."
            )
        }

        viewModelScope.launch {
            try {
                val outputBytes = if (current.mode == ImageEditorMode.CREATE) {
                    if (current.prompt.isBlank()) {
                        throw IllegalArgumentException("Please enter a prompt to create an image.")
                    }
                    if (isOpenAi) {
                        val targetUrl = effectiveConfig.baseUrl.ifBlank { "https://integrate.api.nvidia.com/v1" }
                        openAiService.generateImage(
                            baseUrl = targetUrl,
                            apiKey = effectiveKey,
                            model = targetModel,
                            prompt = current.prompt
                        )
                    } else {
                        geminiService.generateImage(
                            apiKey = effectiveKey,
                            prompt = current.prompt,
                            aspectRatio = current.selectedAspectRatio,
                            model = targetModel
                        )
                    }
                } else {
                    val source = current.sourceImageBytes ?: current.resultImageBytes
                    if (source == null || source.isEmpty()) {
                        throw IllegalArgumentException("Please select an image to edit.")
                    }
                    if (current.editInstruction.isBlank()) {
                        throw IllegalArgumentException("Please enter text instructions on how to edit the image.")
                    }
                    if (isOpenAi) {
                        throw IllegalArgumentException("Modifying existing photos requires Google Gemini Vision. Please switch to Google Gemini in the side panel to edit this photo, or use 'Create Image' mode for FLUX / NVIDIA.")
                    }
                    geminiService.editImage(
                        apiKey = effectiveKey,
                        sourceImageBytes = source,
                        editPrompt = current.editInstruction,
                        aspectRatio = current.selectedAspectRatio,
                        model = targetModel
                    )
                }

                _state.update {
                    it.copy(
                        resultImageBytes = outputBytes,
                        isProcessing = false,
                        stageMessage = null,
                        successMessage = if (current.mode == ImageEditorMode.CREATE) "Image created successfully!" else "Image edited successfully!"
                    )
                }

                // Automatically cache into history so it immediately appears in side panel
                onSaveToHistory?.invoke(
                    "Studio: " + current.prompt.ifBlank { current.editInstruction }.take(25),
                    current.prompt.ifBlank { current.editInstruction },
                    outputBytes
                )
            } catch (e: InvalidApiKeyException) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        stageMessage = null,
                        errorMessage = e.message ?: "Your API key is invalid. Please check Settings."
                    )
                }
            } catch (e: RateLimitException) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        stageMessage = null,
                        errorMessage = e.message ?: "Rate limit reached. Please wait a minute and try again."
                    )
                }
            } catch (e: com.example.data.GeminiApiException) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        stageMessage = null,
                        errorMessage = e.message ?: "API error (HTTP ${e.statusCode})."
                    )
                }
            } catch (e: NoImageGeneratedException) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        stageMessage = null,
                        errorMessage = e.message ?: "Could not process image."
                    )
                }
            } catch (e: IOException) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        stageMessage = null,
                        errorMessage = e.message ?: "Network error. Please check your internet connection."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        stageMessage = null,
                        errorMessage = e.message ?: "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    fun saveToGallery(context: Context, onResult: (Boolean, String) -> Unit) {
        val bytes = _state.value.resultImageBytes ?: return
        if (_state.value.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val saveService = SaveService(context)
            val result = saveService.saveToGallery(bytes, "AI Created Images")

            when (result) {
                is SaveResult.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    onResult(true, "Saved image to your gallery!")
                }
                is SaveResult.PermissionDenied -> {
                    _state.update { it.copy(isSaving = false) }
                    onResult(false, "Photo access permission denied.")
                }
                is SaveResult.Failure -> {
                    _state.update { it.copy(isSaving = false) }
                    onResult(false, result.message)
                }
            }
        }
    }
}
