package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.R
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs

object AlbumArtResolver {

    private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, Uri>()

    private val STANDARD_ART_NAMES = listOf(
        "cover", "folder", "album", "albumart", "front", "artwork", "art"
    )

    private val JPG_EXTENSIONS = listOf("jpg", "jpeg")

    /**
     * Resolves the *.jpg album cover for the given song.
     * Looks for:
     * 1. In-memory cache
     * 2. Existing valid albumArtUri on the song
     * 3. Companion *.jpg in the same directory (e.g. song.jpg, cover.jpg, folder.jpg, or any *.jpg)
     * 4. Embedded album art in the audio file, cached as a *.jpg
     * 5. MediaStore album art
     * 6. Built-in rock album cover *.jpg for demo tracks
     */
    suspend fun resolveAlbumArt(context: Context, song: Song?): Uri? = withContext(Dispatchers.IO) {
        if (song == null) return@withContext null

        memoryCache[song.id]?.let { return@withContext it }

        val uri = resolveAlbumArtUncached(context, song)
        if (uri != null) {
            memoryCache[song.id] = uri
        }
        uri
    }

    private fun resolveAlbumArtUncached(context: Context, song: Song): Uri? {

        // 1. Check existing albumArtUri
        song.albumArtUri?.let { uri ->
            try {
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists() && file.length() > 0) return uri
                } else if (uri.scheme == "content" || uri.scheme == "android.resource") {
                    return uri
                }
            } catch (e: Exception) {
                // Continue searching
            }
        }

        // 2. Check for companion *.jpg in the file's folder (direct filesystem)
        try {
            val audioUri = Uri.parse(song.uriString)
            if (audioUri.scheme == "file") {
                val audioFile = File(audioUri.path ?: "")
                findCompanionJpg(audioFile)?.let { jpgFile ->
                    return Uri.fromFile(jpgFile)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 3. If MediaStore song, check DATA column for disk path and companion *.jpg, or album art URI
        if (song.uriString.startsWith("content://media/")) {
            try {
                val mediaStoreUri = Uri.parse(song.uriString)
                val projection = arrayOf(
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                context.contentResolver.query(mediaStoreUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (dataIdx >= 0) {
                            val dataPath = cursor.getString(dataIdx)
                            if (!dataPath.isNullOrBlank()) {
                                val audioFile = File(dataPath)
                                findCompanionJpg(audioFile)?.let { jpgFile ->
                                    return Uri.fromFile(jpgFile)
                                }
                            }
                        }

                        val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                        if (albumIdIdx >= 0) {
                            val albumId = cursor.getLong(albumIdIdx)
                            if (albumId > 0) {
                                val artUri = ContentUris.withAppendedId(
                                    Uri.parse("content://media/external/audio/albumart"),
                                    albumId
                                )
                                try {
                                    context.contentResolver.openInputStream(artUri)?.use {
                                        return artUri
                                    }
                                } catch (e: Exception) {
                                    // Not found in media store table
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // 4. Extract embedded picture via MediaMetadataRetriever and save as a *.jpg
        try {
            val audioUri = Uri.parse(song.uriString)
            val embeddedJpg = extractEmbeddedArtToJpg(context, audioUri, "art_${abs(song.id.hashCode())}.jpg")
            if (embeddedJpg != null) {
                return Uri.fromFile(embeddedJpg)
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 5. Fallback for built-in tracks if not yet copied to disk
        if (song.isBuiltIn) {
            val resId = when {
                song.title.contains("Electric", ignoreCase = true) -> R.drawable.cover_electric_thunder
                song.title.contains("Highway", ignoreCase = true) -> R.drawable.cover_highway_overdrive
                song.title.contains("Midnight", ignoreCase = true) -> R.drawable.cover_midnight_stomp
                song.title.contains("Rebel", ignoreCase = true) -> R.drawable.cover_rebel_screamer
                else -> R.drawable.cover_electric_thunder
            }
            try {
                val fallbackJpg = File(context.cacheDir, "builtin_${abs(song.title.hashCode())}.jpg")
                if (!fallbackJpg.exists() || fallbackJpg.length() == 0L) {
                    context.resources.openRawResource(resId).use { input ->
                        FileOutputStream(fallbackJpg).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (fallbackJpg.exists() && fallbackJpg.length() > 0) {
                    return Uri.fromFile(fallbackJpg)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return null
    }

    /**
     * Searches the directory containing [audioFile] for companion *.jpg files:
     * 1. <audio_name>.jpg / .jpeg
     * 2. cover.jpg, folder.jpg, album.jpg, etc.
     * 3. Any *.jpg / *.jpeg in that folder
     */
    fun findCompanionJpg(audioFile: File): File? {
        if (!audioFile.exists()) return null
        val parentDir = audioFile.parentFile ?: return null
        if (!parentDir.exists() || !parentDir.isDirectory) return null

        val baseName = audioFile.nameWithoutExtension.lowercase(Locale.ROOT)

        // 1. Same filename with .jpg or .jpeg
        for (ext in JPG_EXTENSIONS) {
            val candidate = File(parentDir, "${audioFile.nameWithoutExtension}.$ext")
            if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                return candidate
            }
        }

        // 2. Standard album names (cover.jpg, folder.jpg, album.jpg, etc.)
        for (stdName in STANDARD_ART_NAMES) {
            for (ext in JPG_EXTENSIONS) {
                val candidate = File(parentDir, "$stdName.$ext")
                if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                    return candidate
                }
            }
        }

        // 3. Scan directory files for any matching *.jpg
        try {
            val files = parentDir.listFiles() ?: return null

            // Prioritize files starting with base name or standard keywords
            val bestMatch = files.firstOrNull { f ->
                val lower = f.name.lowercase(Locale.ROOT)
                val isJpg = lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                isJpg && (lower.startsWith(baseName) || STANDARD_ART_NAMES.any { lower.contains(it) })
            }
            if (bestMatch != null && bestMatch.length() > 0) return bestMatch

            // Otherwise any *.jpg in that folder
            val anyJpg = files.firstOrNull { f ->
                val lower = f.name.lowercase(Locale.ROOT)
                (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) && f.length() > 0
            }
            if (anyJpg != null) return anyJpg
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    /**
     * Extracts embedded picture from audio URI and writes it to a *.jpg file in cache.
     */
    fun extractEmbeddedArtToJpg(context: Context, audioUri: Uri, targetFileName: String): File? {
        val targetFile = File(context.cacheDir, targetFileName)
        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri)
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                FileOutputStream(targetFile).use { it.write(picture) }
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }
}
