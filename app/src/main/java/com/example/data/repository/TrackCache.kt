package com.example.data.repository

import android.content.Context
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Fast local persistent cache for scanned folder tracks.
 * Provides instant (<10ms) loading of track lists on app start,
 * eliminating the need to re-query the slow Storage Access Framework every launch.
 */
object TrackCache {

    private const val CACHE_FILE_NAME = "folder_tracks_cache.json"

    suspend fun saveCachedTracks(context: Context, folderUriString: String, songs: List<Song>) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            val rootObj = JSONObject()
            rootObj.put("folderUri", folderUriString)
            rootObj.put("timestamp", System.currentTimeMillis())

            val array = JSONArray()
            for (song in songs) {
                val songObj = JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                    put("durationMs", song.durationMs)
                    put("uriString", song.uriString)
                    put("format", song.format)
                    put("isHiRes", song.isHiRes)
                    if (song.albumArtUriString != null) {
                        put("albumArtUriString", song.albumArtUriString)
                    }
                    if (song.folderName != null) {
                        put("folderName", song.folderName)
                    }
                    put("bitrateKbps", song.bitrateKbps)
                    put("sampleRateHz", song.sampleRateHz)
                    put("isBuiltIn", song.isBuiltIn)
                }
                array.put(songObj)
            }
            rootObj.put("tracks", array)

            FileOutputStream(file).use { out ->
                out.write(rootObj.toString().toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadCachedTracks(context: Context, folderUriString: String? = null): List<Song> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Song>()
        try {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists() || file.length() == 0L) return@withContext emptyList()

            val jsonStr = FileInputStream(file).use { input ->
                input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }

            val rootObj = JSONObject(jsonStr)
            val cachedUri = rootObj.optString("folderUri", "")
            if (!folderUriString.isNullOrBlank() && cachedUri != folderUriString) {
                // Folder mismatch
                return@withContext emptyList()
            }

            val array = rootObj.optJSONArray("tracks") ?: return@withContext emptyList()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", "Rock Album"),
                        durationMs = obj.optLong("durationMs", 180000L),
                        uriString = obj.getString("uriString"),
                        format = obj.optString("format", "MP3"),
                        isHiRes = obj.optBoolean("isHiRes", false),
                        albumArtUriString = if (obj.has("albumArtUriString")) obj.getString("albumArtUriString") else null,
                        folderName = if (obj.has("folderName")) obj.getString("folderName") else null,
                        bitrateKbps = obj.optInt("bitrateKbps", 320),
                        sampleRateHz = obj.optInt("sampleRateHz", 44100),
                        isBuiltIn = obj.optBoolean("isBuiltIn", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    fun clearCache(context: Context) {
        try {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
