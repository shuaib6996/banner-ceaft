package com.example.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BannerDatabase
import com.example.data.BannerHistoryRepository
import com.example.data.ContentModeratedException
import com.example.data.DefaultAvatarHelper
import com.example.data.GeminiImageService
import com.example.data.ImageProcessingService
import com.example.data.InvalidApiKeyException
import com.example.data.LogoOverlayService
import com.example.data.NoImageGeneratedException
import com.example.data.RateLimitException
import com.example.data.OpenAiCompatibleService
import com.example.data.SaveResult
import com.example.data.SaveService
import com.example.data.TargetDimensions
import com.example.data.TextOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class BannerCreationState(
    val channelTopic: String = "",
    val selectedMood: String = "Energetic",
    val selectedColors: List<String> = emptyList(),
    val customPromptDetails: String? = "",
    val basePromptFragment: String = "",
    val finalPrompt: String = "",
    val selectedAspectRatio: String = "TV 16:9 Display (2560 x 1440)",
    val targetDeviceMode: String = "Phone Safe", // "Phone Safe" (1546x423), "Desktop" (2560x423), "Full TV" (2560x1440)
    val selectedStyle: String = "3D Render",
    val wantsText: Boolean = true,
    val textPosition: String? = "Center",
    val textContent: String? = "Alex Gaming HD",
    val subtextContent: String? = "New videos every Tuesday!",
    val useAiTextInImage: Boolean = false,
    val wantsLogo: Boolean = true,
    val logoFile: String? = "Alex_Avatar_Cutout.png",
    val logoPosition: String? = "Left of Title",

    // Step 5, 6, 7, 8, 9 & 10 Extensions
    val generatedImageBytes: ByteArray? = null,
    val processedImageBytes: ByteArray? = null,
    val masterCanvasBytes: ByteArray? = null,
    val logoImageBytes: ByteArray? = null,
    val finalImageBytes: ByteArray? = null,
    val generationErrorMessage: String? = null,
    val generationStageMessage: String? = null,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val saveErrorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BannerCreationState

        if (channelTopic != other.channelTopic) return false
        if (selectedMood != other.selectedMood) return false
        if (selectedColors != other.selectedColors) return false
        if (customPromptDetails != other.customPromptDetails) return false
        if (basePromptFragment != other.basePromptFragment) return false
        if (finalPrompt != other.finalPrompt) return false
        if (selectedAspectRatio != other.selectedAspectRatio) return false
        if (targetDeviceMode != other.targetDeviceMode) return false
        if (selectedStyle != other.selectedStyle) return false
        if (wantsText != other.wantsText) return false
        if (textPosition != other.textPosition) return false
        if (textContent != other.textContent) return false
        if (subtextContent != other.subtextContent) return false
        if (useAiTextInImage != other.useAiTextInImage) return false
        if (wantsLogo != other.wantsLogo) return false
        if (logoFile != other.logoFile) return false
        if (logoPosition != other.logoPosition) return false
        if (generationErrorMessage != other.generationErrorMessage) return false
        if (generationStageMessage != other.generationStageMessage) return false
        if (isGenerating != other.isGenerating) return false
        if (isSaving != other.isSaving) return false
        if (saveErrorMessage != other.saveErrorMessage) return false

        if (generatedImageBytes != null) {
            if (other.generatedImageBytes == null) return false
            if (!generatedImageBytes.contentEquals(other.generatedImageBytes)) return false
        } else if (other.generatedImageBytes != null) return false

        if (processedImageBytes != null) {
            if (other.processedImageBytes == null) return false
            if (!processedImageBytes.contentEquals(other.processedImageBytes)) return false
        } else if (other.processedImageBytes != null) return false

        if (masterCanvasBytes != null) {
            if (other.masterCanvasBytes == null) return false
            if (!masterCanvasBytes.contentEquals(other.masterCanvasBytes)) return false
        } else if (other.masterCanvasBytes != null) return false

        if (logoImageBytes != null) {
            if (other.logoImageBytes == null) return false
            if (!logoImageBytes.contentEquals(other.logoImageBytes)) return false
        } else if (other.logoImageBytes != null) return false

        if (finalImageBytes != null) {
            if (other.finalImageBytes == null) return false
            if (!finalImageBytes.contentEquals(other.finalImageBytes)) return false
        } else if (other.finalImageBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = channelTopic.hashCode()
        result = 31 * result + selectedMood.hashCode()
        result = 31 * result + selectedColors.hashCode()
        result = 31 * result + (customPromptDetails?.hashCode() ?: 0)
        result = 31 * result + basePromptFragment.hashCode()
        result = 31 * result + finalPrompt.hashCode()
        result = 31 * result + selectedAspectRatio.hashCode()
        result = 31 * result + targetDeviceMode.hashCode()
        result = 31 * result + selectedStyle.hashCode()
        result = 31 * result + wantsText.hashCode()
        result = 31 * result + (textPosition?.hashCode() ?: 0)
        result = 31 * result + (textContent?.hashCode() ?: 0)
        result = 31 * result + (subtextContent?.hashCode() ?: 0)
        result = 31 * result + useAiTextInImage.hashCode()
        result = 31 * result + wantsLogo.hashCode()
        result = 31 * result + (logoFile?.hashCode() ?: 0)
        result = 31 * result + (logoPosition?.hashCode() ?: 0)
        result = 31 * result + (generatedImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (processedImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (masterCanvasBytes?.contentHashCode() ?: 0)
        result = 31 * result + (logoImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (finalImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (generationErrorMessage?.hashCode() ?: 0)
        result = 31 * result + (generationStageMessage?.hashCode() ?: 0)
        result = 31 * result + isGenerating.hashCode()
        result = 31 * result + isSaving.hashCode()
        result = 31 * result + (saveErrorMessage?.hashCode() ?: 0)
        return result
    }
}

class BannerCreationViewModel : ViewModel() {
    private val _state = MutableStateFlow(BannerCreationState())
    val state: StateFlow<BannerCreationState> = _state.asStateFlow()

    private val geminiImageService = GeminiImageService()
    private val openAiService = OpenAiCompatibleService()
    private val huggingFaceService = com.example.data.HuggingFaceImageService()
    private val cloudflareService = com.example.data.CloudflareImageService()
    private val imageProcessingService = ImageProcessingService()
    private val textOverlayService = TextOverlayService()
    private val logoOverlayService = LogoOverlayService()

    init {
        buildBasePrompt()
        buildFinalPrompt()
    }

    /**
     * Pure string construction function converting channel topic, mood, colors,
     * and custom details into a base prompt fragment.
     */
    fun buildBasePrompt(): String {
        val current = _state.value
        val topic = current.channelTopic.trim()
        val mood = current.selectedMood.trim()
        val colors = current.selectedColors
        val details = current.customPromptDetails?.trim() ?: ""

        val isAccordingToPrompt = topic.equals("According to Prompt", ignoreCase = true)
        val prompt = if ((topic.isBlank() || isAccordingToPrompt) && details.isNotBlank()) {
            val moodClause = if (mood.isNotBlank()) ", in a $mood aesthetic" else ""
            val colorClause = if (colors.isNotEmpty()) ", incorporating ${colors.joinToString(", ")} accent color tones" else ""
            "$details$moodClause$colorClause"
        } else {
            val topicText = if (topic.isBlank() || isAccordingToPrompt) "a YouTube channel" else topic
            val colorClause = if (colors.isNotEmpty()) {
                ", incorporating ${colors.joinToString(", ")} accent color tones"
            } else ""

            val detailsClause = if (details.isNotBlank()) {
                ". Additional creator notes: $details"
            } else ""

            "A $mood themed background design for $topicText$colorClause$detailsClause."
        }

        _state.update { it.copy(basePromptFragment = prompt) }
        return prompt
    }

    /**
     * Rebuilds the complete AI prompt string from scratch by combining basePromptFragment,
     * style keywords, aspect-ratio guidance, and text/logo safe-space instructions.
     */
    fun buildFinalPrompt(): String {
        val current = _state.value
        var base = current.basePromptFragment
        if (base.isBlank()) {
            base = buildBasePrompt()
        }

        val styleKw = STYLE_KEYWORDS_MAP[current.selectedStyle]
            ?: "${current.selectedStyle.lowercase()} style, high detail, balanced composition"

        val promptParts = mutableListOf<String>()

        // 1. Mandatory Safe Boundary & Format Guidance - PREPENDED AT START so AI diffusion engines frame the shot first!
        val targetMode = current.targetDeviceMode
        if (targetMode.contains("mobile", ignoreCase = true) || targetMode.contains("phone", ignoreCase = true) || targetMode.contains("1546", ignoreCase = true)) {
            promptParts.add(
                "MANDATORY ULTRA-WIDE HORIZONTAL LETTERBOX FORMAT FOR YOUTUBE MOBILE BANNER (1546x423 SAFE ZONE COMPOSITION). " +
                "COMPOSITION RULES: All visual subjects, typography, titles, logos, characters, and focal graphics MUST be strictly centered within a compact horizontal strip occupying only the middle 20% to 25% vertical height of the image canvas. " +
                "The top 38% and bottom 38% of the canvas MUST be pure ambient background, negative space, dark lighting, or soft atmospheric textures ONLY, completely empty of any text, heads, or foreground subjects, ensuring zero content is cut off or cropped on mobile screens."
            )
        } else if (targetMode.contains("desktop", ignoreCase = true)) {
            promptParts.add(
                "MANDATORY ULTRA-WIDE HORIZONTAL FORMAT FOR YOUTUBE DESKTOP BANNER (2560x423 horizontal strip). " +
                "COMPOSITION RULES: Concentrate all primary subjects, typography, titles, and focal graphics strictly within the central horizontal band (maximum 28% vertical canvas height). " +
                "Top and bottom 36% margins must remain empty ambient atmospheric background with zero text or subjects."
            )
        } else {
            promptParts.add("YouTube Banner canvas (2560x1440 16:9), with all primary subjects centered within the safe viewing zone.")
        }

        promptParts.add(base)
        promptParts.add("Visual style: $styleKw.")

        // Text space / AI text instructions
        if (current.wantsText) {
            if (current.useAiTextInImage && !current.textContent.isNullOrBlank()) {
                promptParts.add("Include legible overlay title text reading '${current.textContent.trim()}' styled as a sleek, compact, single-line horizontal center title strictly inside the middle 20% vertical safe zone, never touching the top or bottom edges.")
            } else {
                val textPosKey = current.textPosition ?: "Center"
                val textSpace = POSITION_SPACE_MAP[textPosKey] ?: "the $textPosKey area simple and uncluttered"
                promptParts.add("Please leave $textSpace.")
            }
        }

        // Logo space instructions
        if (current.wantsLogo) {
            val logoPosKey = current.logoPosition ?: "Left of Title"
            val logoSpace = POSITION_SPACE_MAP[logoPosKey] ?: "the $logoPosKey area simple for an avatar logo"
            promptParts.add("Keep $logoSpace.")
        }

        // Quality clause
        promptParts.add("Masterpiece quality, professional YouTube channel banner, perfectly framed, no watermark, no borders.")

        val completePrompt = promptParts.joinToString(" ")
        _state.update { it.copy(finalPrompt = completePrompt) }
        return completePrompt
    }

    /**
     * Step 6: Processes raw generated image bytes into exact target pixel size (2560x1440).
     */
    suspend fun processGeneratedImage(): Boolean {
        val rawBytes = _state.value.generatedImageBytes ?: return false
        val target = TargetDimensions.getDimensionForRatio(_state.value.selectedAspectRatio)

        return try {
            val processedBytes = imageProcessingService.cropToExactSize(
                sourceBytes = rawBytes,
                targetWidth = target.width,
                targetHeight = target.height
            )

            _state.update {
                it.copy(
                    processedImageBytes = processedBytes,
                    generationErrorMessage = null
                )
            }
            true
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    generationErrorMessage = "Something went wrong preparing your banner. Please try again."
                )
            }
            false
        }
    }

    /**
     * Step 7: Path B Native Text Overlay with Readability Scrim Panel.
     */
    suspend fun applyTextOverlay(): Boolean {
        val current = _state.value
        val sourceBg = current.processedImageBytes ?: current.generatedImageBytes

        if (sourceBg == null) return false

        if (!current.wantsText || current.useAiTextInImage || current.textContent.isNullOrBlank()) {
            _state.update { it.copy(finalImageBytes = sourceBg) }
            return true
        }

        val target = TargetDimensions.getDimensionForRatio(current.selectedAspectRatio)

        return try {
            val overlaidBytes = textOverlayService.overlayText(
                backgroundBytes = sourceBg,
                text = current.textContent.trim(),
                subtext = current.subtextContent?.trim(),
                position = current.textPosition ?: "Center",
                canvasWidth = target.width,
                canvasHeight = target.height
            )

            _state.update {
                it.copy(
                    finalImageBytes = overlaidBytes,
                    generationErrorMessage = null
                )
            }
            true
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    generationErrorMessage = "Couldn't add your text. Please try again."
                )
            }
            false
        }
    }

    /**
     * Step 8: Logo / Avatar Overlay Composition with Safe-Zone Collision Handling.
     */
    suspend fun applyLogoOverlay(): Boolean {
        val current = _state.value
        if (!current.wantsLogo) return true

        val sourceBg = current.finalImageBytes ?: current.processedImageBytes ?: current.generatedImageBytes
            ?: return false

        val effectiveLogoBytes = current.logoImageBytes
            ?: DefaultAvatarHelper.generateDefaultAvatarBytes(current.textContent ?: "A")

        val target = TargetDimensions.getDimensionForRatio(current.selectedAspectRatio)

        return try {
            val combinedBytes = logoOverlayService.overlayLogo(
                backgroundBytes = sourceBg,
                logoBytes = effectiveLogoBytes,
                position = current.logoPosition ?: "Left of Title",
                textPosition = current.textPosition,
                wantsText = current.wantsText,
                canvasWidth = target.width,
                canvasHeight = target.height
            )

            _state.update {
                it.copy(
                    finalImageBytes = combinedBytes,
                    generationErrorMessage = null
                )
            }
            true
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    generationErrorMessage = "Couldn't add your logo. Please try again."
                )
            }
            false
        }
    }

    /**
     * Step 9: Saves banner image bytes to the device gallery, cropped to the exact
     * dimensions of the selected YouTube target device (1546x423 for Mobile, 2560x423 for Desktop, 2560x1440 for Full TV).
     */
    fun saveBanner(context: Context, targetDevice: String? = null, onResult: (Boolean, String) -> Unit) {
        if (_state.value.isSaving) return

        val effectiveDevice = targetDevice ?: _state.value.targetDeviceMode
        val master = _state.value.masterCanvasBytes
        val currentFinal = _state.value.finalImageBytes
            ?: _state.value.processedImageBytes
            ?: _state.value.generatedImageBytes

        if (master == null && (currentFinal == null || currentFinal.isEmpty())) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSaving = true,
                    saveErrorMessage = null
                )
            }

            try {
                // Crop to exact selected YouTube device resolution (1546x423 for Mobile, 2560x423 for Desktop, 2560x1440 for Full)
                val croppedBytes = if (master != null && master.isNotEmpty()) {
                    imageProcessingService.cropToYouTubeTarget(master, effectiveDevice)
                } else {
                    imageProcessingService.cropToYouTubeTarget(currentFinal!!, effectiveDevice)
                }
                val target = com.example.data.YouTubeBannerTarget.fromLabel(effectiveDevice)

                val saveService = SaveService(context)
                val result = saveService.saveToGallery(croppedBytes, "YT Banners")

                when (result) {
                    is SaveResult.Success -> {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                saveErrorMessage = null
                            )
                        }
                        onResult(true, "Saved ${target.title} banner (${target.width} × ${target.height}) to gallery!")
                    }
                    is SaveResult.PermissionDenied -> {
                        val msg = "Photo access is turned off for this app. Enable it in your phone's Settings to save banners."
                        _state.update {
                            it.copy(
                                isSaving = false,
                                saveErrorMessage = msg
                            )
                        }
                        onResult(false, msg)
                    }
                    is SaveResult.Failure -> {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                saveErrorMessage = result.message
                            )
                        }
                        onResult(false, result.message)
                    }
                }
            } catch (e: Exception) {
                val msg = "Error saving banner: ${e.message}"
                _state.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = msg
                    )
                }
                onResult(false, msg)
            }
        }
    }

    /**
     * Dynamically switches active preview and target output between Phone Safe (1546x423),
     * Desktop (2560x423), and Full TV (2560x1440) by re-cropping from the master canvas.
     */
    fun switchTargetDevice(mode: String) {
        val master = _state.value.masterCanvasBytes ?: return
        viewModelScope.launch {
            try {
                val cropped = imageProcessingService.cropToYouTubeTarget(master, mode)
                _state.update {
                    it.copy(
                        targetDeviceMode = mode,
                        finalImageBytes = cropped
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(targetDeviceMode = mode) }
            }
        }
    }

    fun saveBanner(context: Context, onResult: (Boolean, String) -> Unit) {
        saveBanner(context, null, onResult)
    }


    fun restoreBannerFromHistory(bytes: ByteArray, title: String) {
        _state.update {
            it.copy(
                finalImageBytes = bytes,
                textContent = title,
                generationErrorMessage = null,
                isGenerating = false
            )
        }
    }

    fun updateLogoBytes(bytes: ByteArray?, fileName: String? = null) {
        _state.update {
            it.copy(
                logoImageBytes = bytes,
                logoFile = fileName ?: it.logoFile
            )
        }
    }

    fun updateTextAndRedraw(title: String?, subtext: String?) {
        updateTextContent(title, subtext)
        viewModelScope.launch {
            applyTextOverlay()
            applyLogoOverlay()
        }
    }

    /**
     * Step 10 Consolidated Pipeline:
     * Full progress stage messaging + memory hygiene + Room database history caching
     */
    fun generateBanner(
        apiKey: String?,
        context: Context? = null,
        model: String = com.example.data.AiModelConfig.DEFAULT_MODEL,
        providerConfig: com.example.data.ApiProviderConfig? = null
    ) {
        if (_state.value.isGenerating) return

        val effectiveKey = providerConfig?.apiKey?.ifBlank { apiKey.orEmpty() } ?: apiKey.orEmpty()
        val allowBlankKey = providerConfig?.providerId == "ollama"

        if (effectiveKey.isBlank() && !allowBlankKey) {
            _state.update {
                it.copy(
                    isGenerating = false,
                    generationErrorMessage = "API key is required. Please add it in Settings."
                )
            }
            return
        }

        // Set isGenerating immediately on caller thread to prevent double-click race conditions
        _state.update {
            it.copy(
                isGenerating = true,
                generationErrorMessage = null,
                generationStageMessage = "Creating your background...",
                generatedImageBytes = null,
                processedImageBytes = null,
                finalImageBytes = null
            )
        }

        viewModelScope.launch {
            val rawPrompt = buildFinalPrompt()

            // Smart prompt optimization: Imagen 3 :predict endpoint has internal prompt
            // length limits (~480 tokens). When prompt exceeds ~1000 chars, compress it
            // to core visual elements only, dropping verbose layout instructions.
            val prompt = optimizePromptForImageGeneration(rawPrompt)

            try {
                // Stage 1: Raw AI Image Generation (Cloudflare, Hugging Face, OpenAI-compatible, or Gemini)
                val isCloudflare = providerConfig?.providerId == "cloudflare" ||
                    providerConfig?.baseUrl?.contains("cloudflare", ignoreCase = true) == true ||
                    (providerConfig?.imageModel?.startsWith("@cf/", ignoreCase = true) == true)
                val isHuggingFaceKey = effectiveKey.startsWith("hf_", ignoreCase = true)
                val isNvidiaKey = effectiveKey.startsWith("nvapi-", ignoreCase = true)
                val isGoogleKey = effectiveKey.startsWith("AIzaSy")

                val imageBytes = if (isCloudflare) {
                    val targetModel = providerConfig?.imageModel?.ifBlank { "@cf/black-forest-labs/flux-1-schnell" } ?: "@cf/black-forest-labs/flux-1-schnell"
                    cloudflareService.generateImage(
                        baseUrl = providerConfig?.baseUrl.orEmpty(),
                        apiKey = effectiveKey,
                        model = targetModel,
                        prompt = prompt,
                        onProgress = { msg ->
                            _state.update { it.copy(generationStageMessage = msg) }
                        }
                    )
                } else if (isHuggingFaceKey || providerConfig?.providerId == "huggingface") {
                    val targetModel = providerConfig?.imageModel?.ifBlank { "black-forest-labs/FLUX.1-schnell" } ?: "black-forest-labs/FLUX.1-schnell"
                    val targetProvider = providerConfig?.baseUrl?.ifBlank { "auto" } ?: "auto"
                    huggingFaceService.generateImage(
                        apiKey = effectiveKey,
                        model = targetModel,
                        prompt = prompt,
                        provider = targetProvider,
                        onProgress = { msg ->
                            _state.update { it.copy(generationStageMessage = msg) }
                        }
                    )
                } else if (isNvidiaKey || (providerConfig != null && providerConfig.isOpenAiCompatible && !isGoogleKey)) {
                    val targetUrl = providerConfig?.baseUrl?.ifBlank { "https://integrate.api.nvidia.com/v1" } ?: "https://integrate.api.nvidia.com/v1"
                    val targetModel = providerConfig?.imageModel?.ifBlank { "qwen-image" } ?: "qwen-image"
                    openAiService.generateImage(
                        baseUrl = targetUrl,
                        apiKey = effectiveKey,
                        model = targetModel,
                        prompt = prompt
                    )
                } else {
                    var geminiModel = if (providerConfig?.providerId == "gemini" && providerConfig.imageModel.isNotBlank()) {
                        providerConfig.imageModel
                    } else model

                    // Smart routing: if prompt is very detailed (> 600 chars) and user is on
                    // Imagen 3 (:predict), prefer gemini-3.1-flash-image (:generateContent)
                    // which handles longer prompts much better without truncation or 400 errors.
                    if (prompt.length > 600 && geminiModel.startsWith("imagen", ignoreCase = true)) {
                        android.util.Log.i("BannerCreation", "Prompt length ${prompt.length} > 600 chars. Routing from '$geminiModel' to 'gemini-3.1-flash-image' for better long-prompt support.")
                        geminiModel = "gemini-3.1-flash-image"
                    }

                    geminiImageService.generateImage(
                        apiKey = effectiveKey,
                        prompt = prompt,
                        aspectRatio = _state.value.selectedAspectRatio,
                        model = geminiModel
                    )
                }
                _state.update {
                    it.copy(
                        generatedImageBytes = imageBytes,
                        generationStageMessage = "Framing it perfectly..."
                    )
                }

                // Stage 2: 2560x1440 Cover-Fit Cropping
                val processed = processGeneratedImage()

                // Stage 3: Text Overlay Rendering (if applicable)
                if (processed && _state.value.wantsText && !_state.value.useAiTextInImage) {
                    _state.update { it.copy(generationStageMessage = "Adding your text...") }
                    applyTextOverlay()
                }

                // Stage 4: Logo Overlay Composition (if applicable)
                if (processed && _state.value.wantsLogo) {
                    _state.update { it.copy(generationStageMessage = "Placing your logo...") }
                    applyLogoOverlay()
                }

                // Stage 5: Target Device Calibration (Phone Safe 1546x423, Desktop 2560x423, Full TV 2560x1440)
                _state.update { it.copy(generationStageMessage = "Calibrating target resolution...") }

                val master2560 = _state.value.finalImageBytes
                    ?: _state.value.processedImageBytes
                    ?: _state.value.generatedImageBytes
                    ?: imageBytes

                val targetMode = _state.value.targetDeviceMode
                val finalCroppedBytes = if (master2560.isNotEmpty()) {
                    try {
                        imageProcessingService.cropToYouTubeTarget(master2560, targetMode)
                    } catch (e: Exception) {
                        master2560
                    }
                } else {
                    master2560
                }

                _state.update {
                    it.copy(
                        masterCanvasBytes = master2560,
                        finalImageBytes = finalCroppedBytes,
                        generationStageMessage = "Almost ready..."
                    )
                }

                // Auto-save thumbnail to Room history cache if context available
                if (finalCroppedBytes.isNotEmpty() && context != null) {
                    try {
                        val db = BannerDatabase.getDatabase(context)
                        val repo = BannerHistoryRepository(db.bannerHistoryDao(), context)
                        repo.saveBannerToHistory(
                            channelTitle = _state.value.textContent ?: _state.value.channelTopic.ifBlank { "YouTube Banner" },
                            prompt = prompt,
                            imageBytes = finalCroppedBytes
                        )
                    } catch (e: Exception) {
                        // Non-fatal history cache error
                    }
                }

                // Memory Hygiene: Null out intermediate raw bytes once final banner is complete
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generationStageMessage = null,
                        generatedImageBytes = null, // Memory hygiene
                        processedImageBytes = null, // Memory hygiene
                        generationErrorMessage = if (it.finalImageBytes == null && it.generationErrorMessage == null) {
                            "Something went wrong preparing your banner. Please try again."
                        } else it.generationErrorMessage
                    )
                }
            } catch (e: ContentModeratedException) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generationStageMessage = null,
                        generationErrorMessage = e.message ?: "⚠️ Content Moderated: This prompt was flagged by AI safety guidelines. Please modify your prompt and try again."
                    )
                }
            } catch (e: InvalidApiKeyException) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generationStageMessage = null,
                        generationErrorMessage = e.message ?: "Your API key isn't working. Please check it in Settings."
                    )
                }
            } catch (e: RateLimitException) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generationStageMessage = null,
                        generationErrorMessage = e.message ?: "You've hit your usage limit for now. Please wait a bit before trying again."
                    )
                }
            } catch (e: com.example.data.GeminiApiException) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generationStageMessage = null,
                        generationErrorMessage = e.message ?: "API error (HTTP ${e.statusCode}). Please check your settings."
                    )
                }
            } catch (e: IOException) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generationStageMessage = null,
                        generationErrorMessage = e.message?.takeIf { msg -> msg.isNotBlank() }
                            ?: "Couldn't connect. Please check your internet and try again."
                    )
                }
            } catch (e: NoImageGeneratedException) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generationStageMessage = null,
                        generationErrorMessage = e.message ?: "Couldn't create an image from that description. Try adjusting your topic or mood and generate again."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        generationStageMessage = null,
                        generationErrorMessage = e.message ?: "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    fun updateChannelTopic(topic: String) {
        _state.update { it.copy(channelTopic = topic) }
        buildBasePrompt()
        buildFinalPrompt()
    }

    fun updateSelectedMood(mood: String) {
        _state.update { it.copy(selectedMood = mood) }
        buildBasePrompt()
        buildFinalPrompt()
    }

    fun toggleColor(colorHex: String) {
        _state.update { current ->
            val list = current.selectedColors.toMutableList()
            if (list.contains(colorHex)) {
                if (list.size > 1) list.remove(colorHex)
            } else {
                list.add(colorHex)
            }
            current.copy(selectedColors = list)
        }
        buildBasePrompt()
        buildFinalPrompt()
    }

    fun updateCustomPromptDetails(details: String?) {
        _state.update { it.copy(customPromptDetails = details) }
        buildBasePrompt()
        buildFinalPrompt()
    }

    fun updateSelectedAspectRatio(ratio: String) {
        _state.update { it.copy(selectedAspectRatio = ratio) }
        buildFinalPrompt()
    }

    fun updateTargetDeviceMode(mode: String) {
        _state.update { it.copy(targetDeviceMode = mode) }
        buildFinalPrompt()
    }


    fun updateSelectedStyle(style: String) {
        _state.update { it.copy(selectedStyle = style) }
        buildFinalPrompt()
    }

    fun updateWantsText(wants: Boolean) {
        _state.update {
            it.copy(
                wantsText = wants,
                useAiTextInImage = if (wants) false else it.useAiTextInImage
            )
        }
        buildFinalPrompt()
    }

    fun updateTextPosition(position: String?) {
        _state.update { it.copy(textPosition = position) }
        buildFinalPrompt()
    }

    fun updateTextContent(title: String?, subtext: String?) {
        _state.update { it.copy(textContent = title, subtextContent = subtext) }
        buildFinalPrompt()
    }

    fun updateUseAiTextInImage(useAi: Boolean) {
        _state.update { it.copy(useAiTextInImage = useAi) }
        buildFinalPrompt()
    }

    fun updateWantsLogo(wants: Boolean) {
        _state.update { it.copy(wantsLogo = wants) }
        buildFinalPrompt()
    }

    fun updateLogoFile(path: String?) {
        _state.update { it.copy(logoFile = path) }
        buildFinalPrompt()
    }

    fun updateLogoPosition(position: String?) {
        _state.update { it.copy(logoPosition = position) }
        buildFinalPrompt()
    }

    /**
     * Intelligently compresses long prompts for image generation APIs.
     *
     * Problem: When users write detailed prompts AND the system appends style/layout/quality
     * keywords, the combined prompt can exceed 800-1200+ chars. Imagen 3 :predict has an
     * internal ~480 token prompt limit and returns 400 errors. Even Gemini Image models
     * slow down significantly with very long prompts, causing timeouts.
     *
     * Solution: Strip verbose boilerplate instructions (layout, positioning, borders)
     * while preserving the creative/visual core of the user's prompt.
     */
    private fun optimizePromptForImageGeneration(rawPrompt: String): String {
        var optimized = rawPrompt

        // Remove verbose layout instructions that image models ignore anyway
        val boilerplatePatterns = listOf(
            "Please leave the [\\w-]+ area simple and uncluttered\\.?",
            "Keep the [\\w\\s-]+ area simple[^.]*\\.?",
            "Keep the [\\w\\s-]+ simple for an avatar logo\\.?",
            "Designed for 16:9 widescreen canvas \\(2560x1440 YouTube banner\\)\\.?",
            ", no watermark, no borders",
            "professional YouTube banner background design",
            "High quality, professional YouTube banner background design, no watermark, no borders\\.?"
        )

        for (pattern in boilerplatePatterns) {
            optimized = optimized.replace(Regex(pattern, RegexOption.IGNORE_CASE), "")
        }

        // Collapse multiple spaces and dots
        optimized = optimized.replace(Regex("\\s{2,}"), " ")
            .replace(Regex("\\.{2,}"), ".")
            .replace(Regex("\\. \\."), ".")
            .trim()

        // Append compact quality suffix instead of the verbose one if not already present
        if (!optimized.contains("no watermark", ignoreCase = true)) {
            if (!optimized.endsWith(".")) optimized += "."
            val mode = _state.value.targetDeviceMode
            val suffix = if (mode.contains("mobile", ignoreCase = true) || mode.contains("phone", ignoreCase = true) || mode.contains("1546", ignoreCase = true)) {
                " High-quality YouTube banner, horizontal panoramic safe-zone center composition, no watermark."
            } else if (mode.contains("desktop", ignoreCase = true)) {
                " High-quality YouTube banner, wide desktop strip composition, no watermark."
            } else {
                " High-quality YouTube banner, 16:9, no watermark."
            }
            optimized += suffix
        }

        // HARD SAFETY CLAMP: All major image engines (Cloudflare max 2048 chars, Imagen 3 ~480 tokens)
        // fail on ultra-long prompts. Clamp cleanly at sentence or word boundary under 1800 chars.
        if (optimized.length > 1800) {
            val slice = optimized.take(1800)
            val lastBreak = maxOf(
                slice.lastIndexOf(". "),
                slice.lastIndexOf(", "),
                slice.lastIndexOf("\n"),
                slice.lastIndexOf(" ")
            )
            val mode = _state.value.targetDeviceMode
            val clampSuffix = if (mode.contains("mobile", ignoreCase = true) || mode.contains("phone", ignoreCase = true) || mode.contains("1546", ignoreCase = true)) {
                ". High-quality YouTube banner, horizontal panoramic safe-zone center composition, no watermark."
            } else if (mode.contains("desktop", ignoreCase = true)) {
                ". High-quality YouTube banner, wide desktop strip composition, no watermark."
            } else {
                ". High-quality YouTube banner, 16:9, no watermark."
            }
            optimized = if (lastBreak > 1000) {
                slice.substring(0, lastBreak).trimEnd('.', ',', ';', ' ') + clampSuffix
            } else {
                slice.trimEnd()
            }
        }

        android.util.Log.i("BannerCreation", "Prompt optimized: ${rawPrompt.length} → ${optimized.length} chars")

        return optimized
    }

    fun resetToDefaults() {
        _state.value = BannerCreationState()
        buildBasePrompt()
        buildFinalPrompt()
    }

    companion object {
        val STYLE_KEYWORDS_MAP = mapOf(
            "3D Render" to "3D render, soft studio lighting, depth, tactile textures",
            "Studio Real" to "photorealistic, high detail, natural crisp lighting, clean camera shot",
            "Anime Draft" to "expressive anime illustration, vibrant colors, artistic lines",
            "Cyberpunk" to "cyberpunk aesthetic, neon glow, futuristic tech details",
            "Minimal Flat" to "flat 2D vector design, clean geometry, minimal aesthetics",
            "Realistic" to "photorealistic, high detail, natural lighting",
            "Artistic" to "digital painting, artistic brush strokes, expressive",
            "2D Illustration" to "flat 2D illustration, clean vector style",
            "Cartoon" to "cartoon style, bold outlines, vibrant colors"
        )

        val POSITION_SPACE_MAP = mapOf(
            "Top-Left" to "the top-left area simple and uncluttered",
            "Top" to "the top area simple and uncluttered",
            "Top-Right" to "the top-right area simple and uncluttered",
            "Left" to "the left side simple and uncluttered",
            "Center" to "the center area subtle enough for center text and logos to remain readable",
            "Right" to "the right side simple and uncluttered",
            "Bottom-Left" to "the bottom-left area simple and uncluttered",
            "Bottom" to "the bottom area simple and uncluttered",
            "Bottom-Right" to "the bottom-right area simple and uncluttered",
            "Left of Title" to "the left side of the safe zone simple for an avatar logo",
            "Right of Title" to "the right side of the safe zone simple for an avatar logo"
        )
    }
}
