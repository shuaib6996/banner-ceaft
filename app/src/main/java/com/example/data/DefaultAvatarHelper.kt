package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

object DefaultAvatarHelper {

    fun generateDefaultAvatarBytes(title: String = "A"): ByteArray {
        val size = 240
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Circle background fill (CoralRed)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF4B4B")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, bgPaint)

        // White border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, borderPaint)

        // Initial letter
        val letter = title.trim().firstOrNull()?.uppercase() ?: "A"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 108f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val fontMetrics = textPaint.fontMetrics
        val y = size / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(letter, size / 2f, y, textPaint)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }
}
