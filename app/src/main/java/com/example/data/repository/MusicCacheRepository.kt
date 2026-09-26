package com.example.data.repository

import android.content.Context
import com.example.data.db.RockDatabase
import com.example.data.db.ScannedFolderEntity
import com.example.data.db.TrackCacheDao
import com.example.data.db.TrackCacheEntity
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository mediating between Room Database and the UI / Audio Engine.
 * Provides instant zero-latency caching for music scans.
 */
class MusicCacheRepository(
    private val trackCacheDao: TrackCacheDao
) {

    constructor(context: Context) : this(
        RockDatabase.getInstance(context).trackCacheDao()
    )

    fun getAllCachedSongsFlow(): Flow<List<Song>> {
        return trackCacheDao.getAllTracksFlow().map { list ->
            list.map { it.toSong() }
        }
    }

    suspend fun getAllCachedSongs(): List<Song> = withContext(Dispatchers.IO) {
        trackCacheDao.getAllTracks().map { it.toSong() }
    }

    suspend fun getCachedSongsMap(): Map<String, Song> = withContext(Dispatchers.IO) {
        trackCacheDao.getAllTracks().associate { it.uriString to it.toSong() }
    }

    suspend fun getSongsForFolder(folderUri: String): List<Song> = withContext(Dispatchers.IO) {
        trackCacheDao.getTracksByFolderUri(folderUri).map { it.toSong() }
    }

    suspend fun getSongsBySourceType(sourceType: String): List<Song> = withContext(Dispatchers.IO) {
        trackCacheDao.getTracksBySourceType(sourceType).map { it.toSong() }
    }

    suspend fun getTrackByUri(uriString: String): Song? = withContext(Dispatchers.IO) {
        trackCacheDao.getTrackByUri(uriString)?.toSong()
    }

    suspend fun saveScannedSongs(
        songs: List<Song>,
        folderUri: String? = null,
        sourceType: String = "FOLDER"
    ) = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        val entities = songs.map { song ->
            song.toEntity(
                targetFolderUri = folderUri ?: song.folderName,
                targetSourceType = if (song.isUsb) "USB" else if (song.isBuiltIn) "BUILTIN" else sourceType,
                now = now
            )
        }
        trackCacheDao.insertTracks(entities)
    }

    suspend fun updateSong(song: Song) = withContext(Dispatchers.IO) {
        val existing = trackCacheDao.getTrackById(song.id) ?: trackCacheDao.getTrackByUri(song.uriString)
        val folderUri = existing?.folderUri ?: song.folderName
        val sourceType = existing?.sourceType ?: if (song.isUsb) "USB" else if (song.isBuiltIn) "BUILTIN" else "FOLDER"
        val entity = song.toEntity(targetFolderUri = folderUri, targetSourceType = sourceType, now = System.currentTimeMillis())
        trackCacheDao.insertTrack(entity)
    }

    suspend fun removeFolderTracks(folderUri: String) = withContext(Dispatchers.IO) {
        trackCacheDao.deleteTracksByFolderUri(folderUri)
        trackCacheDao.deleteFolder(folderUri)
    }

    suspend fun removeTracksBySourceType(sourceType: String) = withContext(Dispatchers.IO) {
        trackCacheDao.deleteTracksBySourceType(sourceType)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        trackCacheDao.clearAllTracks()
        trackCacheDao.clearAllFolders()
    }

    suspend fun getTrackCount(): Int = withContext(Dispatchers.IO) {
        trackCacheDao.getTrackCount()
    }

    fun getTrackCountFlow(): Flow<Int> {
        return trackCacheDao.getTrackCountFlow()
    }

    // Scanned Folder metadata
    suspend fun saveFolderInfo(
        folderUri: String,
        folderName: String,
        trackCount: Int,
        isUsb: Boolean = false
    ) = withContext(Dispatchers.IO) {
        trackCacheDao.insertFolder(
            ScannedFolderEntity(
                folderUri = folderUri,
                folderName = folderName,
                lastScannedTimestamp = System.currentTimeMillis(),
                trackCount = trackCount,
                isUsb = isUsb
            )
        )
    }

    suspend fun getFolderInfo(folderUri: String): ScannedFolderEntity? = withContext(Dispatchers.IO) {
        trackCacheDao.getFolderByUri(folderUri)
    }

    fun getAllFoldersFlow(): Flow<List<ScannedFolderEntity>> {
        return trackCacheDao.getAllFoldersFlow()
    }

    companion object {
        fun TrackCacheEntity.toSong(): Song {
            return Song(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uriString = uriString,
                format = format,
                isHiRes = isHiRes,
                albumArtUriString = albumArtUriString,
                folderName = folderName,
                bitrateKbps = bitrateKbps,
                sampleRateHz = sampleRateHz,
                isBuiltIn = isBuiltIn,
                isUsb = isUsb
            )
        }

        fun Song.toEntity(
            targetFolderUri: String? = null,
            targetSourceType: String = "FOLDER",
            now: Long = System.currentTimeMillis()
        ): TrackCacheEntity {
            return TrackCacheEntity(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uriString = uriString,
                format = format,
                isHiRes = isHiRes,
                albumArtUriString = albumArtUriString,
                folderName = folderName,
                folderUri = targetFolderUri,
                bitrateKbps = bitrateKbps,
                sampleRateHz = sampleRateHz,
                isBuiltIn = isBuiltIn,
                isUsb = isUsb,
                sourceType = targetSourceType,
                cachedTimestamp = now
            )
        }
    }
}
