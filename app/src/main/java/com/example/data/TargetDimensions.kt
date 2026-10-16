package com.example.data

data class TargetDimension(val width: Int, val height: Int)
data class SafeAreaBounds(val x: Int, val y: Int, val width: Int, val height: Int)

enum class YouTubeBannerTarget(
    val id: String,
    val title: String,
    val width: Int,
    val height: Int,
    val description: String
) {
    MOBILE("mobile", "Phone Safe", 1546, 423, "Mobile safe zone (1546 × 423)"),
    DESKTOP("desktop", "Desktop", 2560, 423, "Desktop display (2560 × 423)"),
    FULL_TV("full", "Full TV", 2560, 1440, "All devices & TV (2560 × 1440)");

    companion object {
        fun fromLabel(label: String): YouTubeBannerTarget {
            return when {
                label.contains("phone", ignoreCase = true) ||
                    label.contains("mobile", ignoreCase = true) ||
                    label.contains("1546", ignoreCase = true) -> MOBILE
                label.contains("desktop", ignoreCase = true) -> DESKTOP
                else -> FULL_TV
            }
        }
    }
}

object TargetDimensions {

    // Primary YouTube Banner Canvas Spec
    val YOUTUBE_BANNER_DIMENSION = TargetDimension(2560, 1440)
    val YOUTUBE_MOBILE_DIMENSION = TargetDimension(1546, 423)
    val YOUTUBE_DESKTOP_DIMENSION = TargetDimension(2560, 423)

    // Standard YouTube 2025 Mobile Safe Zone (1546 x 423 centered in 2560 x 1440 canvas)
    val YOUTUBE_MOBILE_SAFE_AREA = SafeAreaBounds(
        x = (2560 - 1546) / 2, // 507
        y = (1440 - 423) / 2,  // 508
        width = 1546,
        height = 423
    )

    // YouTube Desktop Safe Area (2560 x 423 centered vertically in 2560 x 1440 canvas)
    val YOUTUBE_DESKTOP_SAFE_AREA = SafeAreaBounds(
        x = 0,
        y = (1440 - 423) / 2,  // 508
        width = 2560,
        height = 423
    )

    // YouTube TV Full Canvas
    val YOUTUBE_FULL_SAFE_AREA = SafeAreaBounds(
        x = 0,
        y = 0,
        width = 2560,
        height = 1440
    )

    // Aspect ratio mappings
    val ASPECT_RATIO_DIMENSIONS = mapOf(
        "TV 16:9 Display (2560 x 1440)" to YOUTUBE_BANNER_DIMENSION,
        "16:9" to YOUTUBE_BANNER_DIMENSION,
        "1546:423" to YOUTUBE_MOBILE_DIMENSION,
        "Mobile (1546 x 423)" to YOUTUBE_MOBILE_DIMENSION,
        "2560:423" to YOUTUBE_DESKTOP_DIMENSION,
        "Desktop (2560 x 423)" to YOUTUBE_DESKTOP_DIMENSION,
        "4:3" to TargetDimension(2048, 1536),
        "1:1" to TargetDimension(1080, 1080),
        "9:16" to TargetDimension(1080, 1920)
    )

    fun getDimensionForRatio(rawRatio: String): TargetDimension {
        return ASPECT_RATIO_DIMENSIONS[rawRatio]
            ?: ASPECT_RATIO_DIMENSIONS.entries.firstOrNull { rawRatio.contains(it.key, ignoreCase = true) }?.value
            ?: YOUTUBE_BANNER_DIMENSION
    }
}

