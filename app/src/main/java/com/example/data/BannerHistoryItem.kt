package com.example.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "banner_history")
data class BannerHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelTitle: String,
    val prompt: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePath: String = ""
) {
    @Ignore
    constructor(
        id: Long = 0,
        channelTitle: String,
        prompt: String,
        timestamp: Long = System.currentTimeMillis(),
        imageBytes: ByteArray
    ) : this(id, channelTitle, prompt, timestamp, "")

    @get:Ignore
    val imageBytes: ByteArray
        get() = try {
            if (imagePath.isNotBlank()) {
                val file = File(imagePath)
                if (file.exists()) file.readBytes() else ByteArray(0)
            } else {
                ByteArray(0)
            }
        } catch (_: Exception) {
            ByteArray(0)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BannerHistoryItem

        if (id != other.id) return false
        if (channelTitle != other.channelTitle) return false
        if (prompt != other.prompt) return false
        if (timestamp != other.timestamp) return false
        if (imagePath != other.imagePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + channelTitle.hashCode()
        result = 31 * result + prompt.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + imagePath.hashCode()
        return result
    }
}
