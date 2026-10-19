package com.example.data

import android.graphics.BitmapFactory
import android.util.Log

/**
 * Exception thrown when the AI model flags or blocks a prompt due to content moderation
 * or returns a pitch-black/blank image resulting from safety filter replacements.
 */
class ContentModeratedException(
    message: String = "⚠️ Content Moderated: This prompt was flagged by AI safety guidelines. Please modify or soften your prompt and try again."
) : GeminiException(message)

object ImageSafetyValidator {
    private const val TAG = "ImageSafetyValidator"

    /**
     * Inspects image bytes to detect if the AI returned a pitch-black, blank, or
     * solid color image resulting from safety filter triggers (e.g. FLUX / SD NSFW filter).
     *
     * @return true if image appears to be a legitimate visual image, false if pitch black or blocked.
     */
    fun isImageSafeAndVisible(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false

        return try {
            // Downsample by 8x for fast inspection without large memory allocation
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = 8
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return false

            val width = bitmap.width
            val height = bitmap.height

            if (width <= 0 || height <= 0) return false

            val stepX = (width / 16).coerceAtLeast(1)
            val stepY = (height / 16).coerceAtLeast(1)

            var blackPixels = 0
            var totalSampled = 0
            var maxRgb = 0

            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    if (r > maxRgb) maxRgb = r
                    if (g > maxRgb) maxRgb = g
                    if (b > maxRgb) maxRgb = b

                    // Check if RGB values are practically zero (pure black safety mask)
                    if (r <= 6 && g <= 6 && b <= 6) {
                        blackPixels++
                    }
                    totalSampled++
                }
            }

            if (totalSampled == 0) return true

            val blackRatio = blackPixels.toFloat() / totalSampled

            // A safety filter blackout is completely black across the entire image (maxRgb <= 12 and blackRatio >= 0.98f)
            // Legitimate dark themes (e.g. neon, gaming, dark anime) have glowing accents with maxRgb >> 30
            if (blackRatio >= 0.98f && maxRgb <= 12) {
                Log.w(TAG, "Blocked black image detected! Black pixel ratio: ${(blackRatio * 100).toInt()}%, maxRgb: $maxRgb")
                return false
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking image visibility", e)
            true
        }
    }

    /**
     * Checks if the image is valid and visible. If pitch black or blocked, throws ContentModeratedException.
     */
    @Throws(ContentModeratedException::class)
    fun validateImage(bytes: ByteArray) {
        if (!isImageSafeAndVisible(bytes)) {
            throw ContentModeratedException(
                "⚠️ Content Moderated: Your prompt was flagged by AI safety guidelines and returned a blank/blocked image. Please rephrase or soften your prompt and try again."
            )
        }
    }
}
