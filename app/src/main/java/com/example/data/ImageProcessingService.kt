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

    /**
     * Crops canvas bytes to exact YouTube device dimensions:
     * - Mobile / Phone Safe: 1546 x 423
     * - Desktop: 2560 x 423
     * - Full TV / All Devices: 2560 x 1440
     */
    suspend fun cropToYouTubeTarget(
        sourceBytes: ByteArray,
        target: YouTubeBannerTarget
    ): ByteArray = withContext(Dispatchers.Default) {
        return@withContext try {
            if (target == YouTubeBannerTarget.FULL_TV) {
                return@withContext cropToExactSize(sourceBytes, 2560, 1440)
            }

            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sourceBitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, options)
                ?: throw ImageProcessingException("Failed to decode source image bytes.")

            // If source is already exact target dimensions, return as is
            if (sourceBitmap.width == target.width && sourceBitmap.height == target.height) {
                sourceBitmap.recycle()
                return@withContext sourceBytes
            }

            // Scale to standard 2560 x 1440 canvas
            val canvasBitmap = if (sourceBitmap.width == 2560 && sourceBitmap.height == 1440) {
                sourceBitmap
            } else {
                val scaled = Bitmap.createScaledBitmap(sourceBitmap, 2560, 1440, true)
                if (scaled != sourceBitmap) {
                    sourceBitmap.recycle()
                }
                scaled
            }

            val croppedBitmap = when (target) {
                YouTubeBannerTarget.MOBILE -> {
                    // Mobile Safe Zone Spec: Output is exact 1546 x 423 (ratio ≈ 3.655).
                    // To prevent chopping off the top and bottom of titles, logos, characters, and text,
                    // we sample a centered 2560 x 700 region (ratio 3.657, spanning y=370 to 1070)
                    // and smoothly scale it to 1546 x 423.
                    // This preserves 100% of the focal artwork with generous vertical breathing room!
                    val windowHeight = 700
                    val windowY = (1440 - windowHeight) / 2 // 370
                    val slice = Bitmap.createBitmap(canvasBitmap, 0, windowY, 2560, windowHeight)
                    val scaled = Bitmap.createScaledBitmap(slice, 1546, 423, true)
                    if (slice != scaled && slice != canvasBitmap) {
                        slice.recycle()
                    }
                    scaled
                }
                YouTubeBannerTarget.DESKTOP -> {
                    // Desktop Banner Spec: Output is exact 2560 x 423 (ratio ≈ 6.05).
                    // Sample a centered 2560 x 540 region (y=450 to 990) and scale to 2560 x 423,
                    // ensuring wide desktop text & artwork are never clipped at the edges.
                    val windowHeight = 540
                    val windowY = (1440 - windowHeight) / 2 // 450
                    val slice = Bitmap.createBitmap(canvasBitmap, 0, windowY, 2560, windowHeight)
                    val scaled = Bitmap.createScaledBitmap(slice, 2560, 423, true)
                    if (slice != scaled && slice != canvasBitmap) {
                        slice.recycle()
                    }
                    scaled
                }
                YouTubeBannerTarget.FULL_TV -> {
                    canvasBitmap
                }
            }

            if (canvasBitmap != sourceBitmap && canvasBitmap != croppedBitmap) {
                canvasBitmap.recycle()
            }
            if (sourceBitmap != croppedBitmap) {
                sourceBitmap.recycle()
            }

            ByteArrayOutputStream().use { baos ->
                croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                croppedBitmap.recycle()
                baos.toByteArray()
            }
        } catch (e: ImageProcessingException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "YouTube target crop error", e)
            throw ImageProcessingException("Failed to crop YouTube banner: ${e.message}", e)
        }
    }

    suspend fun cropToYouTubeTarget(
        sourceBytes: ByteArray,
        modeLabel: String
    ): ByteArray {
        val target = YouTubeBannerTarget.fromLabel(modeLabel)
        return cropToYouTubeTarget(sourceBytes, target)
    }

    companion object {
        private const val TAG = "ImageProcessingService"
    }
}

