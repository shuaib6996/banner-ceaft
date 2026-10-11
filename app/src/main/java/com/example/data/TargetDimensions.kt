package com.example.data

data class TargetDimension(val width: Int, val height: Int)
data class SafeAreaBounds(val x: Int, val y: Int, val width: Int, val height: Int)

object TargetDimensions {

    // Primary YouTube Banner Spec
    val YOUTUBE_BANNER_DIMENSION = TargetDimension(2560, 1440)

    // Standard YouTube 2025 Mobile Safe Zone (1546 x 423 centered in 2560 x 1440 canvas)
    val YOUTUBE_MOBILE_SAFE_AREA = SafeAreaBounds(
        x = (2560 - 1546) / 2, // 507
        y = (1440 - 423) / 2,  // 508
        width = 1546,
        height = 423
    )

    // Aspect ratio mappings
    val ASPECT_RATIO_DIMENSIONS = mapOf(
        "TV 16:9 Display (2560 x 1440)" to YOUTUBE_BANNER_DIMENSION,
        "16:9" to YOUTUBE_BANNER_DIMENSION,
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
