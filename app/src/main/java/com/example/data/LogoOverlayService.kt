package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

class LogoOverlayService {

    suspend fun overlayLogo(
        backgroundBytes: ByteArray,
        logoBytes: ByteArray,
        position: String = "Left of Title",
        textPosition: String? = "Center",
        wantsText: Boolean = true,
        canvasWidth: Int = 2560,
        canvasHeight: Int = 1440
    ): ByteArray = withContext(Dispatchers.Main) {
        try {
            val options = BitmapFactory.Options().apply {
                inMutable = true
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bgBitmap = BitmapFactory.decodeByteArray(backgroundBytes, 0, backgroundBytes.size, options)
                ?: return@withContext backgroundBytes

            val logoBitmap = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)
                ?: return@withContext backgroundBytes

            val workingBitmap = if (bgBitmap.isMutable) bgBitmap else bgBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(workingBitmap)

            val safeBounds = TargetDimensions.YOUTUBE_MOBILE_SAFE_AREA

            val logoWidth = logoBitmap.width.toFloat()
            val logoHeight = logoBitmap.height.toFloat()

            if (logoWidth <= 0 || logoHeight <= 0) return@withContext backgroundBytes

            // Max bounding height for logo inside safe zone (~55% of 423px safe height = ~230px)
            val maxBoundingHeight = safeBounds.height * 0.55f
            val maxBoundingWidth = safeBounds.width * 0.25f

            // Contain-fit scale calculation
            val scale = min(maxBoundingWidth / logoWidth, maxBoundingHeight / logoHeight)
            val drawWidth = logoWidth * scale
            val drawHeight = logoHeight * scale

            val safeCenterY = safeBounds.y + safeBounds.height / 2f

            // Determine X, Y draw coordinates inside safe area
            val (drawX, drawY) = when {
                position.equals("Left of Title", ignoreCase = true) || position.equals("Left", ignoreCase = true) -> {
                    val x = safeBounds.x + 40f
                    val y = safeCenterY - (drawHeight / 2f)
                    Pair(x, y)
                }
                position.equals("Right of Title", ignoreCase = true) || position.equals("Right", ignoreCase = true) -> {
                    val x = (safeBounds.x + safeBounds.width) - drawWidth - 40f
                    val y = safeCenterY - (drawHeight / 2f)
                    Pair(x, y)
                }
                position.equals("Top-Left", ignoreCase = true) -> {
                    Pair(safeBounds.x + 30f, safeBounds.y + 20f)
                }
                position.equals("Bottom-Left", ignoreCase = true) -> {
                    Pair(safeBounds.x + 30f, (safeBounds.y + safeBounds.height) - drawHeight - 20f)
                }
                else -> {
                    val x = safeBounds.x + 50f
                    val y = safeCenterY - (drawHeight / 2f)
                    Pair(x, y)
                }
            }

            val destRect = RectF(drawX, drawY, drawX + drawWidth, drawY + drawHeight)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                isDither = true
            }

            // Draw Logo on Canvas with alpha transparency preserved
            canvas.drawBitmap(logoBitmap, null, destRect, paint)

            logoBitmap.recycle()

            ByteArrayOutputStream().use { baos ->
                workingBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                if (workingBitmap != bgBitmap) workingBitmap.recycle()
                bgBitmap.recycle()
                baos.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error overlaying logo", e)
            backgroundBytes
        }
    }

    companion object {
        private const val TAG = "LogoOverlayService"
    }
}
