package com.example.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContentModeratedException
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
    val selectedAspectRatio: String = "Phone Safe",
    val sourceImageBytes: ByteArray? = null,
    val resultImageBytes: ByteArray? = null,
    val masterCanvasBytes: ByteArray? = null,
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

        if (masterCanvasBytes != null) {
            if (other.masterCanvasBytes == null) return false
            if (!masterCanvasBytes.contentEquals(other.masterCanvasBytes)) return false
        } else if (other.masterCanvasBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prompt.hashCode()
        result = 31 * result + editInstruction.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + selectedAspectRatio.hashCode()
        result = 31 * result + (sourceImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (resultImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (masterCanvasBytes?.contentHashCode() ?: 0)
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
    private val huggingFaceService = com.example.data.HuggingFaceImageService()
    private val cloudflareService = com.example.data.CloudflareImageService()
    private val imageProcessingService = com.example.data.ImageProcessingService()

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

    fun switchDeviceTarget(modeLabel: String) {
        val master = _state.value.masterCanvasBytes ?: _state.value.resultImageBytes ?: return
        viewModelScope.launch {
            try {
                val cropped = imageProcessingService.cropToYouTubeTarget(master, modeLabel)
                _state.update {
                    it.copy(
                        resultImageBytes = cropped,
                        selectedAspectRatio = modeLabel
                    )
                }
            } catch (e: Exception) {
                // retain existing
            }
        }
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
        val isCloudflare = effectiveConfig.providerId == "cloudflare" ||
            effectiveConfig.baseUrl.contains("cloudflare", ignoreCase = true) ||
            targetModel.startsWith("@cf/", ignoreCase = true)
        val isHuggingFace = effectiveKey.startsWith("hf_", ignoreCase = true) || effectiveConfig.providerId == "huggingface"
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

        val isYouTubePreset = current.selectedAspectRatio.contains("1546") ||
            current.selectedAspectRatio.contains("2560") ||
            current.selectedAspectRatio.contains("Mobile", ignoreCase = true) ||
            current.selectedAspectRatio.contains("Phone", ignoreCase = true) ||
            current.selectedAspectRatio.contains("Desktop", ignoreCase = true) ||
            current.selectedAspectRatio.contains("Banner", ignoreCase = true)

        val effectiveRatio = if (isYouTubePreset) "16:9" else current.selectedAspectRatio

        val promptToSend = if (isYouTubePreset && current.mode == ImageEditorMode.CREATE) {
            "Cinematic 16:9 widescreen YouTube banner artwork, balanced horizontal composition with focal subjects centered: " + current.prompt.trim()
        } else {
            current.prompt
        }

        viewModelScope.launch {
            try {
                val rawBytes = if (current.mode == ImageEditorMode.CREATE) {
                    if (current.prompt.isBlank()) {
                        throw IllegalArgumentException("Please enter a prompt to create an image.")
                    }
                    if (isCloudflare) {
                        cloudflareService.generateImage(
                            baseUrl = effectiveConfig.baseUrl,
                            apiKey = effectiveKey,
                            model = targetModel.ifBlank { "@cf/black-forest-labs/flux-1-schnell" },
                            prompt = promptToSend,
                            onProgress = { msg ->
                                _state.update { it.copy(stageMessage = msg) }
                            }
                        )
                    } else if (isHuggingFace) {
                        val targetProvider = effectiveConfig.baseUrl.ifBlank { "auto" }
                        huggingFaceService.generateImage(
                            apiKey = effectiveKey,
                            model = targetModel.ifBlank { "black-forest-labs/FLUX.1-schnell" },
                            prompt = promptToSend,
                            provider = targetProvider,
                            onProgress = { msg ->
                                _state.update { it.copy(stageMessage = msg) }
                            }
                        )
                    } else if (isOpenAi) {
                        val targetUrl = effectiveConfig.baseUrl.ifBlank { "https://integrate.api.nvidia.com/v1" }
                        openAiService.generateImage(
                            baseUrl = targetUrl,
                            apiKey = effectiveKey,
                            model = targetModel,
                            prompt = promptToSend
                        )
                    } else {
                        // Smart routing: Imagen :predict has shorter prompt limits (~480 tokens).
                        // For long/detailed prompts, prefer gemini-3.1-flash-image which
                        // handles verbose descriptions much better via :generateContent.
                        val effectiveModel = if (promptToSend.length > 600 && targetModel.startsWith("imagen", ignoreCase = true)) {
                            android.util.Log.i("ImageEditor", "Long prompt (${promptToSend.length} chars) detected. Routing from '$targetModel' to 'gemini-3.1-flash-image'.")
                            "gemini-3.1-flash-image"
                        } else {
                            targetModel
                        }
                        geminiService.generateImage(
                            apiKey = effectiveKey,
                            prompt = promptToSend,
                            aspectRatio = effectiveRatio,
                            model = effectiveModel
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
                        aspectRatio = effectiveRatio,
                        model = targetModel
                    )
                }

                val (outputBytes, masterBytes) = if (isYouTubePreset && rawBytes.isNotEmpty()) {
                    val master = imageProcessingService.cropToExactSize(rawBytes, 2560, 1440)
                    val cropped = if (current.selectedAspectRatio.contains("1546") || current.selectedAspectRatio.contains("Mobile", ignoreCase = true) || current.selectedAspectRatio.contains("Phone", ignoreCase = true)) {
                        imageProcessingService.cropToYouTubeTarget(master, com.example.data.YouTubeBannerTarget.MOBILE)
                    } else if (current.selectedAspectRatio.contains("2560") || current.selectedAspectRatio.contains("Desktop", ignoreCase = true)) {
                        imageProcessingService.cropToYouTubeTarget(master, com.example.data.YouTubeBannerTarget.DESKTOP)
                    } else {
                        master
                    }
                    Pair(cropped, master)
                } else {
                    Pair(rawBytes, null)
                }

                _state.update {
                    it.copy(
                        resultImageBytes = outputBytes,
                        masterCanvasBytes = masterBytes,
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
            } catch (e: ContentModeratedException) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        stageMessage = null,
                        errorMessage = e.message ?: "⚠️ Content Moderated: This prompt was flagged by AI safety guidelines. Please modify your prompt and try again."
                    )
                }
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

    fun saveToGallery(context: Context, targetDevice: String? = null, onResult: (Boolean, String) -> Unit) {
        val currentBytes = _state.value.resultImageBytes ?: return
        if (_state.value.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val saveService = SaveService(context)

            val bytesToSave = if (targetDevice != null) {
                val master = _state.value.masterCanvasBytes ?: currentBytes
                try {
                    imageProcessingService.cropToYouTubeTarget(master, targetDevice)
                } catch (e: Exception) {
                    currentBytes
                }
            } else {
                currentBytes
            }

            val result = saveService.saveToGallery(bytesToSave, "YT Banners")

            when (result) {
                is SaveResult.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    onResult(true, "Saved YouTube Banner (2560 × 1440 - Safe Zone Calibrated) to your gallery! 🎉")
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

    fun saveToGallery(context: Context, onResult: (Boolean, String) -> Unit) {
        saveToGallery(context, null, onResult)
    }

}
