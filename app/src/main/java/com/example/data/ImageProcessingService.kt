package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

class ImageProcessingException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ImageProcessingService {

    suspend fun cropToExactSize(
        sourceBytes: ByteArray,
        targetWidth: Int = 2560,
        targetHeight: Int = 1440
    ): ByteArray = withContext(Dispatchers.Default) {
        return@withContext try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sourceBitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, options)
                ?: throw ImageProcessingException("Failed to decode source image bytes.")

            val srcWidth = sourceBitmap.width
            val srcHeight = sourceBitmap.height

            // Calculate cover-fit scale factor
            val scale = max(
                targetWidth.toFloat() / srcWidth,
                targetHeight.toFloat() / srcHeight
            )

            val scaledWidth = max(1, (srcWidth * scale).toInt())
            val scaledHeight = max(1, (srcHeight * scale).toInt())

            // Smooth scale (bilinear interpolation)
            val scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, scaledWidth, scaledHeight, true)
            if (scaledBitmap != sourceBitmap) {
                sourceBitmap.recycle()
            }

            // Calculate center crop offsets
            val cropX = max(0, (scaledWidth - targetWidth) / 2)
            val cropY = max(0, (scaledHeight - targetHeight) / 2)

            val actualWidth = minOf(targetWidth, scaledWidth - cropX)
            val actualHeight = minOf(targetHeight, scaledHeight - cropY)

            val croppedBitmap = Bitmap.createBitmap(scaledBitmap, cropX, cropY, actualWidth, actualHeight)
            if (croppedBitmap != scaledBitmap) {
                scaledBitmap.recycle()
            }

            ByteArrayOutputStream().use { baos ->
                croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                croppedBitmap.recycle()
                baos.toByteArray()
            }
        } catch (e: ImageProcessingException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image processing error", e)
            throw ImageProcessingException("Error processing image: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "ImageProcessingService"
    }
}
