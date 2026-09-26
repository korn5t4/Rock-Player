package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a cached audio track for instant zero-latency scanning.
 * Indexed by URI, folder URI, and source type for microsecond lookups.
 */
@Entity(
    tableName = "track_cache",
    indices = [
        Index(value = ["uriString"], unique = true),
        Index(value = ["folderUri"]),
        Index(value = ["sourceType"]),
        Index(value = ["artist"]),
        Index(value = ["album"])
    ]
)
data class TrackCacheEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val format: String = "MP3",
    val isHiRes: Boolean = false,
    val albumArtUriString: String? = null,
    val folderName: String? = null,
    val folderUri: String? = null,
    val fileSize: Long = 0L,
    val lastModified: Long = 0L,
    val bitrateKbps: Int = 320,
    val sampleRateHz: Int = 44100,
    val isBuiltIn: Boolean = false,
    val isUsb: Boolean = false,
    val sourceType: String = "FOLDER", // "FOLDER", "MEDIASTORE", "USB", "BUILTIN"
    val cachedTimestamp: Long = System.currentTimeMillis()
)
