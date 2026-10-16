package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class BannerHistoryRepository(
    private val dao: BannerHistoryDao,
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "BannerHistoryRepo"
    }

    val recentBanners: Flow<List<BannerHistoryItem>> = dao.getRecentBanners()
        .catch { e ->
            Log.e(TAG, "Error reading banner history, emitting empty list to prevent crash", e)
            emit(emptyList())
        }

    suspend fun saveBannerToHistory(channelTitle: String, prompt: String, imageBytes: ByteArray) = withContext(Dispatchers.IO) {
        val imagePath = try {
            if (context != null && imageBytes.isNotEmpty()) {
                val bannerDir = File(context.filesDir, "banner_history").apply { if (!exists()) mkdirs() }
                val fileName = "banner_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
                val file = File(bannerDir, fileName)
                file.writeBytes(imageBytes)
                file.absolutePath
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist banner image to disk", e)
            ""
        }

        val item = BannerHistoryItem(
            channelTitle = channelTitle,
            prompt = prompt,
            timestamp = System.currentTimeMillis(),
            imagePath = imagePath
        )
        dao.insertBanner(item)
        dao.trimOldBanners()
    }

    suspend fun deleteBanner(id: Long) = withContext(Dispatchers.IO) {
        try {
            val item = dao.getBannerById(id)
            if (item != null && item.imagePath.isNotBlank()) {
                val file = File(item.imagePath)
                if (file.exists()) file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete banner file for id $id", e)
        }
        dao.deleteBannerById(id)
    }

    suspend fun deleteBanners(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) {
            ids.forEach { id ->
                try {
                    val item = dao.getBannerById(id)
                    if (item != null && item.imagePath.isNotBlank()) {
                        val file = File(item.imagePath)
                        if (file.exists()) file.delete()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete banner file for id $id", e)
                }
            }
            dao.deleteBannersByIds(ids)
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            if (context != null) {
                val bannerDir = File(context.filesDir, "banner_history")
                if (bannerDir.exists()) {
                    bannerDir.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete banner history directory", e)
        }
        dao.clearHistory()
    }
}
