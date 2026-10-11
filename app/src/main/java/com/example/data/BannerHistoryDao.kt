package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BannerHistoryDao {

    @Query("SELECT * FROM banner_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentBanners(): Flow<List<BannerHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(item: BannerHistoryItem)

    @Query("DELETE FROM banner_history WHERE id NOT IN (SELECT id FROM banner_history ORDER BY timestamp DESC LIMIT 10)")
    suspend fun trimOldBanners()

    @Query("DELETE FROM banner_history WHERE id = :id")
    suspend fun deleteBannerById(id: Long)

    @Query("DELETE FROM banner_history")
    suspend fun clearHistory()
}
