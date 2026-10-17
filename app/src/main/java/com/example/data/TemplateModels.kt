package com.example.data

enum class TemplateSlotType {
    TEXT,
    IMAGE;

    companion object {
        fun fromString(str: String): TemplateSlotType {
            return when (str.trim().lowercase()) {
                "image", "photo", "logo" -> IMAGE
                else -> TEXT
            }
        }
    }
}

data class SlotRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class SlotTextStyle(
    val fontSize: Float = 64f,
    val colorHex: String = "#FFFFFF",
    val isBold: Boolean = true,
    val alignment: String = "CENTER" // "LEFT", "CENTER", "RIGHT"
)

data class TemplateSlot(
    val id: String,
    val type: TemplateSlotType,
    val label: String,
    val rect: SlotRect,
    val defaultStyle: SlotTextStyle? = null,
    val required: Boolean = true
)

data class BannerTemplate(
    val id: String,
    val name: String,
    val category: String,
    val description: String = "",
    val thumbnailAsset: String,
    val backgroundAsset: String,
    val slots: List<TemplateSlot>
)
