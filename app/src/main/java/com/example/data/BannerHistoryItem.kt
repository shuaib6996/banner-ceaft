package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "banner_history")
data class BannerHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelTitle: String,
    val prompt: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BannerHistoryItem

        if (id != other.id) return false
        if (channelTitle != other.channelTitle) return false
        if (prompt != other.prompt) return false
        if (timestamp != other.timestamp) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + channelTitle.hashCode()
        result = 31 * result + prompt.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + imageBytes.contentHashCode()
        return result
    }
}
