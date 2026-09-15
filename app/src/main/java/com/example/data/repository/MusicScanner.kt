package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs

object MusicScanner {

    private val SUPPORTED_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "aac", "ogg", "m4a", "opus", "alac", "aiff", "wma"
    )

    private val STANDARD_ART_NAMES = listOf(
        "cover", "folder", "album", "albumart", "front", "artwork"
    )

    fun isSupportedAudioFile(fileName: String, mimeType: String? = null): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (SUPPORTED_EXTENSIONS.contains(ext)) return true
        if (mimeType != null && mimeType.startsWith("audio/")) return true
        return false
    }

    private fun isJpgImageFile(fileName: String, mimeType: String? = null): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext == "jpg" || ext == "jpeg") return true
        if (mimeType != null && (mimeType == "image/jpeg" || mimeType == "image/jpg")) return true
        return false
    }

    suspend fun scanDocumentTreeUri(context: Context, treeUri: Uri): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        try {
            // Take persistable permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }

            scanUriRecursively(context, treeUri, songs, maxDepth = 6, currentDepth = 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        songs
    }

    private data class FileDoc(
        val docId: String,
        val displayName: String,
        val mimeType: String,
        val uri: Uri
    )

    private fun scanUriRecursively(
        context: Context,
        folderUri: Uri,
        outList: MutableList<Song>,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth) return

        val contentResolver = context.contentResolver
        val childrenUri = try {
            android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                android.provider.DocumentsContract.getDocumentId(folderUri)
            )
        } catch (e: Exception) {
            null
        } ?: return

        val projection = arrayOf(
            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
            android.provider.DocumentsContract.Document.COLUMN_SIZE
        )

        val audioDocs = mutableListOf<FileDoc>()
        val jpgDocs = mutableListOf<FileDoc>()

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(childrenUri, projection, null, null, null)
            if (cursor != null) {
                val idIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIndex)
                    val displayName = cursor.getString(nameIndex) ?: "Unknown"
                    val mimeType = cursor.getString(mimeIndex) ?: ""

                    val childDocUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)

                    if (mimeType == android.provider.DocumentsContract.Document.MIME_TYPE_DIR) {
                        // Recurse into subfolder!
                        scanUriRecursively(context, childDocUri, outList, maxDepth, currentDepth + 1)
                    } else if (isJpgImageFile(displayName, mimeType)) {
                        jpgDocs.add(FileDoc(docId, displayName, mimeType, childDocUri))
                    } else if (isSupportedAudioFile(displayName, mimeType)) {
                        audioDocs.add(FileDoc(docId, displayName, mimeType, childDocUri))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        // Match companion *.jpg for each audio file in this directory
        val folderName = folderUri.lastPathSegment ?: "Folder"
        for (audioDoc in audioDocs) {
            val audioBaseName = audioDoc.displayName.substringBeforeLast('.').lowercase(Locale.ROOT)

            // Look for matching *.jpg:
            // 1. Same filename as audio (e.g. song.jpg)
            // 2. Standard album name (cover.jpg, folder.jpg, albumart.jpg)
            // 3. Any *.jpg in the folder
            val matchedJpg = jpgDocs.firstOrNull { jpg ->
                jpg.displayName.substringBeforeLast('.').equals(audioBaseName, ignoreCase = true)
            } ?: jpgDocs.firstOrNull { jpg ->
                val lower = jpg.displayName.lowercase(Locale.ROOT)
                STANDARD_ART_NAMES.any { lower.contains(it) }
            } ?: jpgDocs.firstOrNull()

            val song = extractMetadata(
                context = context,
                fileUri = audioDoc.uri,
                fallbackName = audioDoc.displayName,
                folderName = folderName,
                preferredArtUri = matchedJpg?.uri
            )
            outList.add(song)
        }
    }

    suspend fun scanDeviceMediaStore(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                val albumIdCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                    val title = it.getString(titleCol) ?: "Unknown Track"
                    val artist = it.getString(artistCol).takeIf { a -> a != "<unknown>" } ?: "Unknown Artist"
                    val album = it.getString(albumCol).takeIf { a -> a != "<unknown>" } ?: "Rock Album"
                    val duration = it.getLong(durCol)
                    val displayName = it.getString(nameCol) ?: title
                    val mime = it.getString(mimeCol) ?: "audio/mpeg"

                    val ext = displayName.substringAfterLast('.', "").uppercase(Locale.ROOT)
                    val format = when {
                        mime.contains("flac", ignoreCase = true) || ext == "FLAC" -> "FLAC"
                        mime.contains("wav", ignoreCase = true) || ext == "WAV" -> "WAV"
                        mime.contains("aac", ignoreCase = true) || ext == "AAC" -> "AAC"
                        mime.contains("ogg", ignoreCase = true) || ext == "OGG" -> "OGG"
                        mime.contains("mp4", ignoreCase = true) || ext == "M4A" -> "M4A"
                        else -> "MP3"
                    }
                    val isHiRes = format == "FLAC" || format == "WAV"

                    // Resolve *.jpg for this track
                    var artUriString: String? = null

                    // 1. Check disk folder for companion *.jpg
                    if (dataCol >= 0) {
                        val path = it.getString(dataCol)
                        if (!path.isNullOrBlank()) {
                            val audioFile = File(path)
                            val companionJpg = AlbumArtResolver.findCompanionJpg(audioFile)
                            if (companionJpg != null) {
                                artUriString = Uri.fromFile(companionJpg).toString()
                            }
                        }
                    }

                    // 2. Check MediaStore album art URI
                    if (artUriString == null && albumIdCol >= 0) {
                        val albumId = it.getLong(albumIdCol)
                        if (albumId > 0) {
                            val albumArtUri = ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            )
                            try {
                                context.contentResolver.openInputStream(albumArtUri)?.use {
                                    artUriString = albumArtUri.toString()
                                }
                            } catch (e: Exception) {
                                // Not available in media store
                            }
                        }
                    }

                    // 3. Extract embedded picture if not found
                    if (artUriString == null) {
                        val embeddedJpg = AlbumArtResolver.extractEmbeddedArtToJpg(context, contentUri, "art_${id}.jpg")
                        if (embeddedJpg != null) {
                            artUriString = Uri.fromFile(embeddedJpg).toString()
                        }
                    }

                    songs.add(
                        Song(
                            id = "mediastore_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            uriString = contentUri.toString(),
                            format = format,
                            isHiRes = isHiRes,
                            albumArtUriString = artUriString,
                            folderName = "Device Music",
                            bitrateKbps = if (isHiRes) 1411 else 320,
                            sampleRateHz = if (isHiRes) 48000 else 44100
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songs
    }

    private fun extractMetadata(
        context: Context,
        fileUri: Uri,
        fallbackName: String,
        folderName: String,
        preferredArtUri: Uri? = null
    ): Song {
        val retriever = MediaMetadataRetriever()
        var title = fallbackName.substringBeforeLast('.')
        var artist = "Unknown Artist"
        var album = "Unknown Album"
        var durationMs = 180000L
        var bitrate = 320
        var albumArtUriStr: String? = preferredArtUri?.toString()

        try {
            retriever.setDataSource(context, fileUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                if (it.isNotBlank()) title = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                if (it.isNotBlank()) artist = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                if (it.isNotBlank()) album = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                if (it > 0) durationMs = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let {
                if (it > 0) bitrate = it / 1000
            }

            if (albumArtUriStr == null) {
                val picture = retriever.embeddedPicture
                if (picture != null) {
                    val artFile = File(context.cacheDir, "art_${abs(fileUri.hashCode())}.jpg")
                    FileOutputStream(artFile).use { it.write(picture) }
                    albumArtUriStr = Uri.fromFile(artFile).toString()
                }
            }
        } catch (e: Exception) {
            // Use defaults
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }

        val ext = fallbackName.substringAfterLast('.', "").uppercase(Locale.ROOT)
        val format = when (ext) {
            "FLAC" -> "FLAC"
            "WAV" -> "WAV"
            "AAC" -> "AAC"
            "OGG" -> "OGG"
            "M4A" -> "M4A"
            "OPUS" -> "OPUS"
            else -> "MP3"
        }
        val isHiRes = format == "FLAC" || format == "WAV" || bitrate > 500

        return Song(
            id = fileUri.toString(),
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uriString = fileUri.toString(),
            format = format,
            isHiRes = isHiRes,
            albumArtUriString = albumArtUriStr,
            folderName = folderName,
            bitrateKbps = bitrate,
            sampleRateHz = if (isHiRes) 48000 else 44100
        )
    }
}

