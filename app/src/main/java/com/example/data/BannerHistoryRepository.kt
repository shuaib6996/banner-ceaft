package com.example.data

import kotlinx.coroutines.flow.Flow

class BannerHistoryRepository(private val dao: BannerHistoryDao) {

    val recentBanners: Flow<List<BannerHistoryItem>> = dao.getRecentBanners()

    suspend fun saveBannerToHistory(channelTitle: String, prompt: String, imageBytes: ByteArray) {
        val item = BannerHistoryItem(
            channelTitle = channelTitle,
            prompt = prompt,
            timestamp = System.currentTimeMillis(),
            imageBytes = imageBytes
        )
        dao.insertBanner(item)
        dao.trimOldBanners()
    }

    suspend fun deleteBanner(id: Long) {
        dao.deleteBannerById(id)
    }

    suspend fun clearAll() {
        dao.clearHistory()
    }
}
