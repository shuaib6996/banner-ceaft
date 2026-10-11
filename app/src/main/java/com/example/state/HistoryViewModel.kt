package com.example.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BannerDatabase
import com.example.data.BannerHistoryItem
import com.example.data.BannerHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BannerHistoryRepository

    val recentBannersState: StateFlow<List<BannerHistoryItem>>

    init {
        val database = BannerDatabase.getDatabase(application)
        repository = BannerHistoryRepository(database.bannerHistoryDao())
        recentBannersState = repository.recentBanners.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun saveToHistory(channelTitle: String, prompt: String, imageBytes: ByteArray) {
        viewModelScope.launch {
            repository.saveBannerToHistory(channelTitle, prompt, imageBytes)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteBanner(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
