package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
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
     * High-speed, pure Kotlin box blur on ARGB_8888 bitmap.
     * When applied to a downscaled bitmap (e.g. 160x90) and scaled back up,
     * this produces a silky-smooth, artifact-free cinematic blur in < 5ms.
     */
    fun fastBoxBlur(src: Bitmap, radius: Int = 4, passes: Int = 2): Bitmap {
        val w = src.width
        val h = src.height
        val currentPix = IntArray(w * h)
        src.getPixels(currentPix, 0, w, 0, 0, w, h)
        val tempPix = IntArray(w * h)
        val div = 2 * radius + 1

        repeat(passes) {
            // Horizontal Pass
            for (y in 0 until h) {
                val yOffset = y * w
                for (x in 0 until w) {
                    var aSum = 0
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    for (i in -radius..radius) {
                        val cx = (x + i).coerceIn(0, w - 1)
                        val p = currentPix[yOffset + cx]
                        aSum += (p ushr 24) and 0xff
                        rSum += (p ushr 16) and 0xff
                        gSum += (p ushr 8) and 0xff
                        bSum += p and 0xff
                    }
                    tempPix[yOffset + x] = ((aSum / div) shl 24) or
                            ((rSum / div) shl 16) or
                            ((gSum / div) shl 8) or
                            (bSum / div)
                }
            }

            // Vertical Pass
            for (x in 0 until w) {
                for (y in 0 until h) {
                    var aSum = 0
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    for (i in -radius..radius) {
                        val cy = (y + i).coerceIn(0, h - 1)
                        val p = tempPix[cy * w + x]
                        aSum += (p ushr 24) and 0xff
                        rSum += (p ushr 16) and 0xff
                        gSum += (p ushr 8) and 0xff
                        bSum += p and 0xff
                    }
                    currentPix[y * w + x] = ((aSum / div) shl 24) or
                            ((rSum / div) shl 16) or
                            ((gSum / div) shl 8) or
                            (bSum / div)
                }
            }
        }

        return Bitmap.createBitmap(currentPix, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Creates a standard 2560x1440 YouTube-compliant banner image:
     * - The output is ALWAYS 2560x1440 so YouTube Studio accepts it immediately without
     *   the "Photo is too small. It must be at least 2048 x 1152 pixels." rejection error.
     * - In Mobile / Desktop modes, the central safe area contains the crisp, full-detail artwork.
     * - The top (y=0..508) and bottom (y=931..1440) areas are blurred and subtly darkened,
     *   with sleek glowing divider lines, exactly matching professional banner aesthetics.
     */
    suspend fun createYouTubeBlurBanner(
        sourceBytes: ByteArray,
        target: YouTubeBannerTarget = YouTubeBannerTarget.MOBILE
    ): ByteArray = withContext(Dispatchers.Default) {
        return@withContext try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sourceBitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, options)
                ?: throw ImageProcessingException("Failed to decode source image bytes.")

            // Ensure source is 2560 x 1440 cover-fit
            val canvasBitmap = if (sourceBitmap.width == 2560 && sourceBitmap.height == 1440) {
                sourceBitmap
            } else {
                val scale = max(2560f / sourceBitmap.width, 1440f / sourceBitmap.height)
                val scaledW = max(1, (sourceBitmap.width * scale).toInt())
                val scaledH = max(1, (sourceBitmap.height * scale).toInt())
                val scaled = Bitmap.createScaledBitmap(sourceBitmap, scaledW, scaledH, true)
                val cropX = max(0, (scaledW - 2560) / 2)
                val cropY = max(0, (scaledH - 1440) / 2)
                val cropped = Bitmap.createBitmap(scaled, cropX, cropY, 2560, 1440)
                if (scaled != sourceBitmap && scaled != cropped) {
                    scaled.recycle()
                }
                if (sourceBitmap != cropped) {
                    sourceBitmap.recycle()
                }
                cropped
            }

            if (target == YouTubeBannerTarget.FULL_TV) {
                // TV target displays full 2560x1440 canvas without blur overlay
                return@withContext ByteArrayOutputStream().use { baos ->
                    canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    if (canvasBitmap != sourceBitmap) canvasBitmap.recycle()
                    baos.toByteArray()
                }
            }

            // Target is MOBILE or DESKTOP: Generate 2560x1440 YouTube-ready banner with blurred top/bottom
            val resultBitmap = Bitmap.createBitmap(2560, 1440, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            // 1. Create blurred atmospheric background from the artwork
            val downscaleW = 160
            val downscaleH = 90
            val downscaled = Bitmap.createScaledBitmap(canvasBitmap, downscaleW, downscaleH, true)
            val blurredSmall = fastBoxBlur(downscaled, radius = 4, passes = 2)
            if (downscaled != blurredSmall) downscaled.recycle()

            val blurredBig = Bitmap.createScaledBitmap(blurredSmall, 2560, 1440, true)
            blurredSmall.recycle()

            // Draw blurred background
            canvas.drawBitmap(blurredBig, 0f, 0f, null)
            blurredBig.recycle()

            // Apply ~30% dark overlay across blurred background for depth and contrast
            val darkOverlayPaint = Paint().apply {
                color = Color.argb(75, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 2560f, 1440f, darkOverlayPaint)

            // 2. Draw the sharp middle safe zone banner strip (height 423, Y: 508..931)
            val safeY = TargetDimensions.YOUTUBE_MOBILE_SAFE_AREA.y // 508
            val safeHeight = TargetDimensions.YOUTUBE_MOBILE_SAFE_AREA.height // 423
            val bottomY = safeY + safeHeight // 931

            val stripSrcRect = Rect(0, safeY, 2560, bottomY)
            val stripDstRect = Rect(0, safeY, 2560, bottomY)
            canvas.drawBitmap(canvasBitmap, stripSrcRect, stripDstRect, null)

            // 3. Draw sleek glowing rim divider lines (as in reference image)
            val cyanGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(95, 0, 220, 255)
                strokeWidth = 4f
                style = Paint.Style.STROKE
            }
            val crispLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(230, 255, 255, 255)
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }

            // Top divider at Y = 508
            canvas.drawLine(0f, safeY.toFloat() - 1f, 2560f, safeY.toFloat() - 1f, cyanGlowPaint)
            canvas.drawLine(0f, safeY.toFloat(), 2560f, safeY.toFloat(), crispLinePaint)

            // Bottom divider at Y = 931
            canvas.drawLine(0f, bottomY.toFloat(), 2560f, bottomY.toFloat(), crispLinePaint)
            canvas.drawLine(0f, bottomY.toFloat() + 1f, 2560f, bottomY.toFloat() + 1f, cyanGlowPaint)

            if (canvasBitmap != sourceBitmap && canvasBitmap != resultBitmap) {
                canvasBitmap.recycle()
            }
            if (sourceBitmap != resultBitmap) {
                sourceBitmap.recycle()
            }

            ByteArrayOutputStream().use { baos ->
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                resultBitmap.recycle()
                baos.toByteArray()
            }
        } catch (e: ImageProcessingException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "YouTube blur banner generation error", e)
            throw ImageProcessingException("Failed to generate YouTube banner: ${e.message}", e)
        }
    }

    /**
     * Primary YouTube banner calibration method:
     * Produces standard 2560x1440 YouTube-compliant banners with blurred top/bottom and
     * crisp safe zone center, 100% compliant with YouTube Studio upload validation (>= 2048x1152).
     */
    suspend fun cropToYouTubeTarget(
        sourceBytes: ByteArray,
        target: YouTubeBannerTarget
    ): ByteArray {
        return createYouTubeBlurBanner(sourceBytes, target)
    }

    suspend fun cropToYouTubeTarget(
        sourceBytes: ByteArray,
        modeLabel: String
    ): ByteArray {
        val target = YouTubeBannerTarget.fromLabel(modeLabel)
        return createYouTubeBlurBanner(sourceBytes, target)
    }

    /**
     * Crops canvas bytes to exact raw device slice dimensions (without canvas padding):
     * - Mobile: 1546 x 423
     * - Desktop: 2560 x 423
     * - Full TV: 2560 x 1440
     * Note: Use this only when exporting standalone slices for Twitter/Discord.
     * YouTube Studio requires full 2560x1440 banners (cropToYouTubeTarget).
     */
    suspend fun cropToExactSlice(
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

            if (sourceBitmap.width == target.width && sourceBitmap.height == target.height) {
                sourceBitmap.recycle()
                return@withContext sourceBytes
            }

            val canvasBitmap = if (sourceBitmap.width == 2560 && sourceBitmap.height == 1440) {
                sourceBitmap
            } else {
                val scale = max(2560f / sourceBitmap.width, 1440f / sourceBitmap.height)
                val scaledW = max(1, (sourceBitmap.width * scale).toInt())
                val scaledH = max(1, (sourceBitmap.height * scale).toInt())
                val scaled = Bitmap.createScaledBitmap(sourceBitmap, scaledW, scaledH, true)
                val cropX = max(0, (scaledW - 2560) / 2)
                val cropY = max(0, (scaledH - 1440) / 2)
                val cropped = Bitmap.createBitmap(scaled, cropX, cropY, 2560, 1440)
                if (scaled != sourceBitmap && scaled != cropped) scaled.recycle()
                if (sourceBitmap != cropped) sourceBitmap.recycle()
                cropped
            }

            val croppedBitmap = when (target) {
                YouTubeBannerTarget.MOBILE -> {
                    val windowHeight = 700
                    val windowY = (1440 - windowHeight) / 2 // 370
                    val slice = Bitmap.createBitmap(canvasBitmap, 0, windowY, 2560, windowHeight)
                    val scaled = Bitmap.createScaledBitmap(slice, 1546, 423, true)
                    if (slice != scaled && slice != canvasBitmap) slice.recycle()
                    scaled
                }
                YouTubeBannerTarget.DESKTOP -> {
                    val windowHeight = 540
                    val windowY = (1440 - windowHeight) / 2 // 450
                    val slice = Bitmap.createBitmap(canvasBitmap, 0, windowY, 2560, windowHeight)
                    val scaled = Bitmap.createScaledBitmap(slice, 2560, 423, true)
                    if (slice != scaled && slice != canvasBitmap) slice.recycle()
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
        } catch (e: Exception) {
            Log.e(TAG, "Exact slice crop error", e)
            throw ImageProcessingException("Failed to crop slice: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "ImageProcessingService"
    }
}

