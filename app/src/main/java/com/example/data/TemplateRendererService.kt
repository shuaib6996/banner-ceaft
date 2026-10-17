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
import kotlin.math.max
import kotlin.math.min

class TemplateRendererService {

    /**
     * Renders user-provided slot values onto the template background bitmap.
     * All coordinates are strictly referenced on the 2560x1440 canvas.
     */
    suspend fun renderTemplate(
        baseBitmap: Bitmap,
        template: BannerTemplate,
        slotValues: Map<String, Any>
    ): ByteArray = withContext(Dispatchers.Default) {
        val workingBitmap = if (baseBitmap.isMutable) {
            baseBitmap
        } else {
            baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val canvas = Canvas(workingBitmap)

        for (slot in template.slots) {
            val value = slotValues[slot.id] ?: continue
            val rect = slot.rect

            when (slot.type) {
                TemplateSlotType.TEXT -> {
                    val textStr = (value as? String)?.trim().orEmpty()
                    if (textStr.isNotEmpty()) {
                        drawTextSlot(canvas, textStr, rect, slot.defaultStyle)
                    }
                }
                TemplateSlotType.IMAGE -> {
                    val imgBytes = value as? ByteArray
                    if (imgBytes != null && imgBytes.isNotEmpty()) {
                        drawImageSlot(canvas, imgBytes, rect)
                    }
                }
            }
        }

        ByteArrayOutputStream().use { baos ->
            workingBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            if (workingBitmap != baseBitmap) {
                workingBitmap.recycle()
            }
            baos.toByteArray()
        }
    }

    /**
     * Draws text cleanly inside the specified slot rect with automatic font downscaling,
     * shadow/readability contrast, and alignment.
     */
    private fun drawTextSlot(
        canvas: Canvas,
        text: String,
        rect: SlotRect,
        style: SlotTextStyle?
    ) {
        val textColor = try {
            Color.parseColor(style?.colorHex ?: "#FFFFFF")
        } catch (e: Exception) {
            Color.WHITE
        }

        val isBold = style?.isBold ?: true
        val baseFontSize = style?.fontSize ?: 68f
        val alignStr = style?.alignment ?: "CENTER"

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = if (isBold) {
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            } else {
                Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            setShadowLayer(8f, 2f, 2f, Color.argb(190, 0, 0, 0))
        }

        var fontSize = baseFontSize
        textPaint.textSize = fontSize

        val maxWidth = rect.width
        val maxHeight = rect.height

        // Downscale font size if single-line measurement exceeds slot width
        while (textPaint.measureText(text) > maxWidth && fontSize > 24f) {
            fontSize -= 2f
            textPaint.textSize = fontSize
        }

        val alignment = when {
            alignStr.contains("LEFT", ignoreCase = true) -> Layout.Alignment.ALIGN_NORMAL
            alignStr.contains("RIGHT", ignoreCase = true) -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }

        val layout = StaticLayout.Builder.obtain(
            text, 0, text.length, textPaint, maxWidth
        ).setAlignment(alignment).build()

        // Center vertically inside rect
        val textY = rect.y + max(0f, (maxHeight - layout.height) / 2f)
        val textX = rect.x.toFloat()

        canvas.save()
        canvas.translate(textX, textY)
        layout.draw(canvas)
        canvas.restore()
    }

    /**
     * Draws an image into the slot rect using contain-fit scaling (preserves aspect ratio, never distorts).
     */
    private fun drawImageSlot(
        canvas: Canvas,
        imageBytes: ByteArray,
        rect: SlotRect
    ) {
        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val logoBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                ?: return

            val imgW = logoBitmap.width.toFloat()
            val imgH = logoBitmap.height.toFloat()
            if (imgW <= 0 || imgH <= 0) return

            // Contain-fit scale calculation
            val scale = min(rect.width.toFloat() / imgW, rect.height.toFloat() / imgH)
            val destW = imgW * scale
            val destH = imgH * scale

            // Center inside slot rect
            val destX = rect.x + (rect.width - destW) / 2f
            val destY = rect.y + (rect.height - destH) / 2f

            val destRect = RectF(destX, destY, destX + destW, destY + destH)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                isDither = true
            }

            canvas.drawBitmap(logoBitmap, null, destRect, paint)
            logoBitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing image slot", e)
        }
    }

    companion object {
        private const val TAG = "TemplateRendererService"
    }
}
