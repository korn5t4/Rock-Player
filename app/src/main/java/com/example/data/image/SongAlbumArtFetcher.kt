package com.example.data.image

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.R
import com.example.data.model.Song
import com.example.data.repository.AlbumArtResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.buffer
import java.io.File
import java.io.InputStream
import kotlin.math.abs

/**
 * Custom Coil Fetcher that directly integrates audio track album art with Coil's DiskCache.
 *
 * How it ensures smooth scrolling & reduced battery usage:
 * 1. Checks Coil's DiskCache first (<1ms local disk hit).
 * 2. If missed, resolves art from companion *.jpg, embedded ID3 tags, or MediaStore on Dispatchers.IO.
 * 3. Immediately commits the artwork bytes into Coil's DiskCache so subsequent list scrolls
 *    never trigger ContentResolver IPC, Storage Access Framework binder transactions, or
 *    MediaMetadataRetriever decodes.
 * 4. Supplies the cached disk file directly to Coil's downsampling decoder pipeline,
 *    allowing hardware-accelerated, low-memory thumbnail rendering.
 */
class SongAlbumArtFetcher(
    private val context: Context,
    private val song: Song,
    private val options: Options,
    private val imageLoader: ImageLoader
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val diskCache = imageLoader.diskCache ?: return@withContext null
        val cacheKey = "album_art_song_${song.id}"

        // 1. Check if already present in Coil's DiskCache
        diskCache.openSnapshot(cacheKey)?.let { snapshot ->
            return@withContext SourceResult(
                source = ImageSource(file = snapshot.data, diskCacheKey = cacheKey, closeable = snapshot),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK
            )
        }

        // 2. Not in disk cache yet. Resolve the artwork stream.
        val inputStream = openArtworkInputStream(song) ?: return@withContext null

        // 3. Write artwork into Coil's DiskCache editor
        val editor = diskCache.openEditor(cacheKey)
        if (editor != null) {
            try {
                FileSystem.SYSTEM.sink(editor.data).buffer().use { sink ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    inputStream.use { stream ->
                        while (stream.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                        }
                    }
                }
                val snapshot = editor.commitAndOpenSnapshot()
                if (snapshot != null) {
                    return@withContext SourceResult(
                        source = ImageSource(file = snapshot.data, diskCacheKey = cacheKey, closeable = snapshot),
                        mimeType = "image/jpeg",
                        dataSource = DataSource.DISK
                    )
                }
            } catch (e: Exception) {
                try {
                    editor.abort()
                } catch (abortEx: Exception) {
                    // Ignore
                }
            }
        }

        null
    }

    private fun openArtworkInputStream(song: Song): InputStream? {
        // A. If song has an explicit albumArtUri
        song.albumArtUri?.let { uri ->
            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream != null) return stream
            } catch (e: Exception) {
                // Continue to fallback
            }
        }

        // B. Companion *.jpg in directory if file-based
        try {
            val audioUri = Uri.parse(song.uriString)
            if (audioUri.scheme == "file") {
                val file = File(audioUri.path ?: "")
                AlbumArtResolver.findCompanionJpg(file)?.let { companionFile ->
                    if (companionFile.exists() && companionFile.length() > 0) {
                        return companionFile.inputStream()
                    }
                }
            }
        } catch (e: Exception) {
            // Continue
        }

        // C. MediaStore album art query if MediaStore content URI
        if (song.uriString.startsWith("content://media/")) {
            try {
                val projection = arrayOf(
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                context.contentResolver.query(Uri.parse(song.uriString), projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                        if (albumIdIdx >= 0) {
                            val albumId = cursor.getLong(albumIdIdx)
                            if (albumId > 0) {
                                val artUri = ContentUris.withAppendedId(
                                    Uri.parse("content://media/external/audio/albumart"),
                                    albumId
                                )
                                context.contentResolver.openInputStream(artUri)?.let { return it }
                            }
                        }

                        val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (dataIdx >= 0) {
                            val path = cursor.getString(dataIdx)
                            if (!path.isNullOrBlank()) {
                                val audioFile = File(path)
                                AlbumArtResolver.findCompanionJpg(audioFile)?.let {
                                    if (it.exists() && it.length() > 0) return it.inputStream()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // D. Extract embedded picture via MediaMetadataRetriever
        try {
            val audioUri = Uri.parse(song.uriString)
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, audioUri)
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    return picture.inputStream()
                }
            } finally {
                try { retriever.release() } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            // Continue
        }

        // E. Built-in rock demo songs fallback
        if (song.isBuiltIn) {
            val resId = when {
                song.title.contains("Electric", ignoreCase = true) -> R.drawable.cover_electric_thunder
                song.title.contains("Highway", ignoreCase = true) -> R.drawable.cover_highway_overdrive
                song.title.contains("Midnight", ignoreCase = true) -> R.drawable.cover_midnight_stomp
                song.title.contains("Rebel", ignoreCase = true) -> R.drawable.cover_rebel_screamer
                else -> R.drawable.cover_electric_thunder
            }
            try {
                return context.resources.openRawResource(resId)
            } catch (e: Exception) {
                // Ignore
            }
        }

        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Song> {
        override fun create(data: Song, options: Options, imageLoader: ImageLoader): Fetcher {
            return SongAlbumArtFetcher(context, data, options, imageLoader)
        }
    }
}
