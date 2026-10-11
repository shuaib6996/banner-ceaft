package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BannerHistoryItem::class], version = 1, exportSchema = false)
abstract class BannerDatabase : RoomDatabase() {

    abstract fun bannerHistoryDao(): BannerHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: BannerDatabase? = null

        fun getDatabase(context: Context): BannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BannerDatabase::class.java,
                    "banner_craft_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
