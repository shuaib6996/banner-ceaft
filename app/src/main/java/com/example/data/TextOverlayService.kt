package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

class TextOverlayService {

    suspend fun overlayText(
        backgroundBytes: ByteArray,
        text: String,
        subtext: String? = null,
        position: String = "Center",
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

            val workingBitmap = if (bgBitmap.isMutable) bgBitmap else bgBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(workingBitmap)

            val safeBounds = TargetDimensions.YOUTUBE_MOBILE_SAFE_AREA

            val trimmedTitle = text.trim()
            val trimmedSub = subtext?.trim() ?: ""

            if (trimmedTitle.isEmpty()) {
                return@withContext backgroundBytes
            }

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0))
            }

            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF4B4B") // CoralRed
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                setShadowLayer(6f, 1f, 1f, Color.argb(160, 0, 0, 0))
            }

            val maxPanelWidth = (safeBounds.width * 0.85f).toInt()
            var fontSize = 96f
            titlePaint.textSize = fontSize

            while (titlePaint.measureText(trimmedTitle) > maxPanelWidth && fontSize > 42f) {
                fontSize -= 4f
                titlePaint.textSize = fontSize
            }

            subPaint.textSize = fontSize * 0.45f

            val titleLayout = StaticLayout.Builder.obtain(
                trimmedTitle, 0, trimmedTitle.length, titlePaint, maxPanelWidth
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

            val subLayout = if (trimmedSub.isNotEmpty()) {
                StaticLayout.Builder.obtain(
                    trimmedSub, 0, trimmedSub.length, subPaint, maxPanelWidth
                ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            } else null

            val totalTextHeight = titleLayout.height + (subLayout?.height ?: 0) + 16

            val paddingX = 40f
            val paddingY = 28f

            val scrimWidth = min(maxPanelWidth + paddingX * 2, safeBounds.width.toFloat())
            val scrimHeight = totalTextHeight + paddingY * 2

            val centerX = safeBounds.x + safeBounds.width / 2f
            val scrimLeft = centerX - scrimWidth / 2f
            val scrimRight = centerX + scrimWidth / 2f

            val scrimTop = when {
                position.contains("Top", ignoreCase = true) -> safeBounds.y + 24f
                position.contains("Bottom", ignoreCase = true) -> (safeBounds.y + safeBounds.height) - scrimHeight - 24f
                else -> (safeBounds.y + safeBounds.height / 2f) - (scrimHeight / 2f)
            }
            val scrimBottom = scrimTop + scrimHeight

            // Readability Scrim Panel (Black ~40% opacity)
            val scrimRect = RectF(scrimLeft, scrimTop, scrimRight, scrimBottom)
            val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(100, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(scrimRect, 24f, 24f, scrimPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(60, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(scrimRect, 24f, 24f, borderPaint)

            val textStartY = scrimTop + paddingY

            canvas.save()
            canvas.translate(centerX - (maxPanelWidth / 2f), textStartY)
            titleLayout.draw(canvas)

            if (subLayout != null) {
                canvas.translate(0f, titleLayout.height + 12f)
                subLayout.draw(canvas)
            }
            canvas.restore()

            ByteArrayOutputStream().use { baos ->
                workingBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                if (workingBitmap != bgBitmap) workingBitmap.recycle()
                bgBitmap.recycle()
                baos.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error overlaying text", e)
            backgroundBytes
        }
    }

    companion object {
        private const val TAG = "TextOverlayService"
    }
}
