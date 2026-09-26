package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for fast music scan caching and metadata persistence.
 */
@Database(
    entities = [TrackCacheEntity::class, ScannedFolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RockDatabase : RoomDatabase() {

    abstract fun trackCacheDao(): TrackCacheDao

    companion object {
        @Volatile
        private var INSTANCE: RockDatabase? = null

        fun getInstance(context: Context): RockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RockDatabase::class.java,
                    "rock_music_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
