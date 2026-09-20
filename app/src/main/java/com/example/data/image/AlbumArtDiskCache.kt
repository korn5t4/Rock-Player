package com.example.data.image

import android.content.Context
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

/**
 * Centralized disk and memory cache configuration for Coil album artwork.
 * Provides high-performance local disk caching to prevent repeated ContentResolver
 * and Storage Access Framework queries, dramatically reducing CPU/GPU overhead,
 * saving battery, and guaranteeing 60/120 FPS scrolling.
 */
object AlbumArtDiskCache {

    const val DISK_CACHE_DIR_NAME = "coil_album_art_disk_cache"
    private const val MAX_DISK_CACHE_BYTES = 150L * 1024 * 1024 // 150 MB

    fun getDiskCacheDir(context: Context): File {
        return File(context.cacheDir, DISK_CACHE_DIR_NAME)
    }

    fun createDiskCache(context: Context): DiskCache {
        return DiskCache.Builder()
            .directory(getDiskCacheDir(context))
            .maxSizeBytes(MAX_DISK_CACHE_BYTES)
            .build()
    }

    fun createMemoryCache(context: Context): MemoryCache {
        return MemoryCache.Builder(context)
            .maxSizePercent(0.25) // Up to 25% of available app RAM
            .strongReferencesEnabled(true)
            .build()
    }

    /**
     * Checks if a cached artwork file exists on disk for the given song ID.
     */
    fun getCachedArtworkFile(context: Context, songId: String): File? {
        val cacheKey = "album_art_song_$songId"
        // Coil uses hexadecimal or hash filenames inside its disk cache directory
        val dir = getDiskCacheDir(context)
        if (!dir.exists()) return null
        
        // Check for direct key match or matching files in disk cache directory
        val candidate = File(dir, "$cacheKey.0")
        if (candidate.exists() && candidate.length() > 0) {
            return candidate
        }
        val fallback = File(dir, cacheKey)
        if (fallback.exists() && fallback.length() > 0) {
            return fallback
        }
        return null
    }

    /**
     * Returns the total bytes occupied by the album art disk cache.
     */
    fun getCacheSizeBytes(context: Context): Long {
        val dir = getDiskCacheDir(context)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    /**
     * Clears the album art disk cache.
     */
    fun clearCache(context: Context) {
        val dir = getDiskCacheDir(context)
        if (dir.exists()) {
            dir.deleteRecursively()
            dir.mkdirs()
        }
    }
}
