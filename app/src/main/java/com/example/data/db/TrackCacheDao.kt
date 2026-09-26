package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for fast track cache operations.
 */
@Dao
interface TrackCacheDao {

    @Query("SELECT * FROM track_cache ORDER BY title ASC")
    fun getAllTracksFlow(): Flow<List<TrackCacheEntity>>

    @Query("SELECT * FROM track_cache ORDER BY title ASC")
    suspend fun getAllTracks(): List<TrackCacheEntity>

    @Query("SELECT COUNT(*) FROM track_cache")
    fun getTrackCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM track_cache")
    suspend fun getTrackCount(): Int

    @Query("SELECT * FROM track_cache WHERE folderUri = :folderUri ORDER BY title ASC")
    suspend fun getTracksByFolderUri(folderUri: String): List<TrackCacheEntity>

    @Query("SELECT * FROM track_cache WHERE sourceType = :sourceType ORDER BY title ASC")
    suspend fun getTracksBySourceType(sourceType: String): List<TrackCacheEntity>

    @Query("SELECT * FROM track_cache WHERE uriString = :uriString LIMIT 1")
    suspend fun getTrackByUri(uriString: String): TrackCacheEntity?

    @Query("SELECT * FROM track_cache WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackCacheEntity)

    @Query("DELETE FROM track_cache WHERE folderUri = :folderUri")
    suspend fun deleteTracksByFolderUri(folderUri: String)

    @Query("DELETE FROM track_cache WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    @Query("DELETE FROM track_cache WHERE sourceType = :sourceType")
    suspend fun deleteTracksBySourceType(sourceType: String)

    @Query("DELETE FROM track_cache")
    suspend fun clearAllTracks()

    // Scanned Folder Queries
    @Query("SELECT * FROM scanned_folders ORDER BY lastScannedTimestamp DESC")
    fun getAllFoldersFlow(): Flow<List<ScannedFolderEntity>>

    @Query("SELECT * FROM scanned_folders WHERE folderUri = :folderUri LIMIT 1")
    suspend fun getFolderByUri(folderUri: String): ScannedFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ScannedFolderEntity)

    @Query("DELETE FROM scanned_folders WHERE folderUri = :folderUri")
    suspend fun deleteFolder(folderUri: String)

    @Query("DELETE FROM scanned_folders")
    suspend fun clearAllFolders()
}
