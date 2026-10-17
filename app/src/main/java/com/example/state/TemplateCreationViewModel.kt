package com.example.state

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BannerTemplate
import com.example.data.ImageProcessingService
import com.example.data.TemplateRendererService
import com.example.data.TemplateRepository
import com.example.data.TemplateSlotType
import com.example.data.YouTubeBannerTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateCreationState(
    val templates: List<BannerTemplate> = emptyList(),
    val selectedCategory: String = "All",
    val selectedTemplate: BannerTemplate? = null,
    val slotValues: Map<String, Any> = emptyMap(),
    val targetDeviceMode: String = "Phone Safe",
    val isRendering: Boolean = false,
    val masterCanvasBytes: ByteArray? = null,
    val finalRenderedBytes: ByteArray? = null,
    val errorMessage: String? = null
) {
    /**
     * True only when every required slot in the selected template has a non-empty value.
     */
    val isReadyToRender: Boolean
        get() {
            val template = selectedTemplate ?: return false
            val requiredSlots = template.slots.filter { it.required }
            if (requiredSlots.isEmpty()) return true

            for (slot in requiredSlots) {
                val value = slotValues[slot.id] ?: return false
                when (slot.type) {
                    TemplateSlotType.TEXT -> {
                        val str = value as? String
                        if (str.isNullOrBlank()) return false
                    }
                    TemplateSlotType.IMAGE -> {
                        val bytes = value as? ByteArray
                        if (bytes == null || bytes.isEmpty()) return false
                    }
                }
            }
            return true
        }

    val hasUnsavedChanges: Boolean
        get() = slotValues.isNotEmpty()
}

class TemplateCreationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TemplateRepository(application.applicationContext)
    private val renderer = TemplateRendererService()
    private val imageProcessingService = ImageProcessingService()

    private val _state = MutableStateFlow(TemplateCreationState())
    val state: StateFlow<TemplateCreationState> = _state.asStateFlow()

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            val list = repository.getTemplates()
            _state.update { it.copy(templates = list) }
        }
    }

    fun selectCategory(category: String) {
        _state.update { it.copy(selectedCategory = category) }
    }

    /**
     * Selects a template and resets slotValues map (Edge case requirement 7).
     */
    fun selectTemplate(template: BannerTemplate) {
        _state.update {
            it.copy(
                selectedTemplate = template,
                slotValues = emptyMap(),
                masterCanvasBytes = null,
                finalRenderedBytes = null,
                errorMessage = null
            )
        }
    }

    /**
     * Sets value for a slot (String for text, ByteArray for image).
     */
    fun setSlotValue(slotId: String, value: Any) {
        _state.update { current ->
            val updated = current.slotValues.toMutableMap()
            updated[slotId] = value
            current.copy(slotValues = updated, errorMessage = null)
        }
    }

    fun clearSlotValue(slotId: String) {
        _state.update { current ->
            val updated = current.slotValues.toMutableMap()
            updated.remove(slotId)
            current.copy(slotValues = updated)
        }
    }

    fun updateTargetDeviceMode(mode: String) {
        _state.update { it.copy(targetDeviceMode = mode) }
    }

    fun resetCustomization() {
        _state.update {
            it.copy(
                slotValues = emptyMap(),
                masterCanvasBytes = null,
                finalRenderedBytes = null,
                errorMessage = null
            )
        }
    }

    /**
     * Composites template background + filled slots into a final 2560x1440 banner,
     * then calibrates for the selected device resolution (1546x423 for Phone Safe, 2560x423 for Desktop, 2560x1440 for Full TV).
     */
    suspend fun renderFinalBanner(
        context: Context,
        targetDevice: String = _state.value.targetDeviceMode
    ): ByteArray? {
        val current = _state.value
        val template = current.selectedTemplate ?: return null

        _state.update { it.copy(isRendering = true, errorMessage = null) }

        return try {
            val bgBitmap = repository.loadAssetBitmap(template.backgroundAsset)
                ?: throw IllegalStateException("Could not load template background image.")

            // 1. Composite all slots onto the 2560x1440 canvas
            val masterBytes = renderer.renderTemplate(
                baseBitmap = bgBitmap,
                template = template,
                slotValues = current.slotValues
            )

            // 2. Crop/scale to target device (Phone Safe 1546x423, Desktop 2560x423, Full TV 2560x1440)
            val target = YouTubeBannerTarget.fromLabel(targetDevice)
            val calibratedBytes = imageProcessingService.cropToYouTubeTarget(masterBytes, target)

            _state.update {
                it.copy(
                    isRendering = false,
                    masterCanvasBytes = masterBytes,
                    finalRenderedBytes = calibratedBytes
                )
            }
            calibratedBytes
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isRendering = false,
                    errorMessage = "Rendering failed: ${e.message}"
                )
            }
            null
        }
    }
}
