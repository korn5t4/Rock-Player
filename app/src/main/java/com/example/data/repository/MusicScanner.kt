package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs

/**
 * High-performance music scanner with persistent SAF authorization and instant scanning.
 */
object MusicScanner {

    private val SUPPORTED_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "aac", "ogg", "m4a", "opus", "alac", "aiff", "wma"
    )

    private val STANDARD_ART_NAMES = listOf(
        "cover", "folder", "album", "albumart", "front", "artwork"
    )

    /**
     * Persists and remembers the SAF URI authorization so the user is never prompted again.
     */
    fun takeAndPersistFolderPermission(context: Context, treeUri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
            true
        } catch (e: Exception) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * Checks if the SAF authorization for the given treeUri is actively retained by the OS.
     */
    fun isFolderPermissionGranted(context: Context, treeUri: Uri): Boolean {
        return try {
            val persisted = context.contentResolver.persistedUriPermissions
            persisted.any { it.uri.toString() == treeUri.toString() && it.isReadPermission }
        } catch (e: Exception) {
            false
        }
    }

    fun isSupportedAudioFile(fileName: String, mimeType: String? = null): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (SUPPORTED_EXTENSIONS.contains(ext)) return true
        if (mimeType != null && mimeType.startsWith("audio/")) return true
        return false
    }

    private fun isJpgImageFile(fileName: String, mimeType: String? = null): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp") return true
        if (mimeType != null && mimeType.startsWith("image/")) return true
        return false
    }

    fun getFolderDisplayName(context: Context, treeUri: Uri): String {
        try {
            val docId = try {
                DocumentsContract.getTreeDocumentId(treeUri)
            } catch (e: Exception) {
                DocumentsContract.getDocumentId(treeUri)
            }
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) return name
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        val lastSegment = treeUri.lastPathSegment
        if (!lastSegment.isNullOrBlank()) {
            val decoded = Uri.decode(lastSegment)
            val clean = if (decoded.contains(":")) {
                decoded.substringAfterLast(':').substringAfterLast('/')
            } else {
                decoded.substringAfterLast('/')
            }
            if (clean.isNotBlank()) return clean
        }
        return "Selected Folder"
    }

    /**
     * Fast tag parser from filename (e.g. "01 - AC_DC - Highway to Hell.mp3" -> "AC/DC", "Highway to Hell").
     * Runs in microseconds with zero disk/IPC overhead.
     */
    fun fastParseTrackDetails(fileName: String, folderName: String): Pair<String, String> {
        val raw = fileName.substringBeforeLast('.')
        // Strip leading track numbers like "01 - ", "01. ", "01 ", "[01] "
        val clean = raw.replaceFirst(Regex("^\\s*(\\[?\\d{1,3}\\]?[.\\-_\\s]+)+"), "").trim()
        return if (clean.contains(" - ")) {
            val parts = clean.split(" - ", limit = 2)
            val artist = parts[0].replace('_', ' ').trim()
            val title = parts[1].replace('_', ' ').trim()
            Pair(if (artist.isNotBlank()) artist else folderName, if (title.isNotBlank()) title else clean)
        } else if (clean.contains(" – ")) { // en-dash
            val parts = clean.split(" – ", limit = 2)
            Pair(parts[0].trim(), parts[1].trim())
        } else if (clean.contains("_-_")) {
            val parts = clean.split("_-_", limit = 2)
            Pair(parts[0].replace('_', ' ').trim(), parts[1].replace('_', ' ').trim())
        } else {
            val title = clean.replace('_', ' ').trim()
            Pair(folderName, if (title.isNotBlank()) title else raw)
        }
    }

    fun isExternalUsbUri(treeUri: Uri): Boolean {
        val uriStr = treeUri.toString()
        if (uriStr.contains("com.android.externalstorage.documents")) {
            val path = treeUri.path ?: ""
            val treeSegment = path.substringAfter("/tree/").substringBefore("%3A").substringBefore(":")
            if (treeSegment.isNotEmpty() && treeSegment != "primary") {
                return true
            }
        }
        return false
    }

    /**
     * Fast document tree scanner with Room database cache acceleration.
     * Takes cached tracks into account for instant zero-latency retrieval.
     */
    suspend fun scanDocumentTreeUri(
        context: Context,
        treeUri: Uri,
        cachedTracksMap: Map<String, Song> = emptyMap(),
        isUsbDevice: Boolean = false
    ): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        try {
            takeAndPersistFolderPermission(context, treeUri)
            val isUsb = isUsbDevice || isExternalUsbUri(treeUri)
            val rootName = getFolderDisplayName(context, treeUri)
            val effectiveRootName = if (isUsb && !rootName.startsWith("USB", ignoreCase = true)) "USB: $rootName" else rootName

            // Automatic Room cache lookup if not provided
            val effectiveCacheMap = if (cachedTracksMap.isNotEmpty()) {
                cachedTracksMap
            } else {
                try {
                    MusicCacheRepository(context).getCachedSongsMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            }

            scanUriRecursivelyFast(
                context = context,
                folderUri = treeUri,
                outList = songs,
                cachedTracksMap = effectiveCacheMap,
                maxDepth = 6,
                currentDepth = 0,
                currentFolderName = effectiveRootName,
                isUsbSource = isUsb
            )

            // Persist newly scanned tracks in Room database for fast future scans
            if (songs.isNotEmpty()) {
                try {
                    val repo = MusicCacheRepository(context)
                    repo.saveScannedSongs(
                        songs = songs,
                        folderUri = treeUri.toString(),
                        sourceType = if (isUsb) "USB" else "FOLDER"
                    )
                    repo.saveFolderInfo(
                        folderUri = treeUri.toString(),
                        folderName = effectiveRootName,
                        trackCount = songs.size,
                        isUsb = isUsb
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        songs
    }

    private data class FileDoc(
        val docId: String,
        val displayName: String,
        val mimeType: String,
        val size: Long,
        val uri: Uri
    )

    private fun scanUriRecursivelyFast(
        context: Context,
        folderUri: Uri,
        outList: MutableList<Song>,
        cachedTracksMap: Map<String, Song>,
        maxDepth: Int,
        currentDepth: Int,
        currentFolderName: String? = null,
        isUsbSource: Boolean = false
    ) {
        if (currentDepth > maxDepth) return

        val contentResolver = context.contentResolver
        val childrenUri = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getDocumentId(folderUri)
            )
        } catch (e: Exception) {
            null
        } ?: return

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        val audioDocs = mutableListOf<FileDoc>()
        val jpgDocs = mutableListOf<FileDoc>()

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(childrenUri, projection, null, null, null)
            if (cursor != null) {
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIndex)
                    val displayName = cursor.getString(nameIndex) ?: "Unknown"
                    val mimeType = cursor.getString(mimeIndex) ?: ""
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L

                    val childDocUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanUriRecursivelyFast(
                            context = context,
                            folderUri = childDocUri,
                            outList = outList,
                            cachedTracksMap = cachedTracksMap,
                            maxDepth = maxDepth,
                            currentDepth = currentDepth + 1,
                            currentFolderName = displayName,
                            isUsbSource = isUsbSource
                        )
                    } else if (isJpgImageFile(displayName, mimeType)) {
                        jpgDocs.add(FileDoc(docId, displayName, mimeType, size, childDocUri))
                    } else if (isSupportedAudioFile(displayName, mimeType)) {
                        audioDocs.add(FileDoc(docId, displayName, mimeType, size, childDocUri))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        val folderName = currentFolderName ?: getFolderDisplayName(context, folderUri)

        for (audioDoc in audioDocs) {
            val uriStr = audioDoc.uri.toString()

            // 1. Check if we already have this exact track in local cache (Instant)
            val cachedSong = cachedTracksMap[uriStr]
            if (cachedSong != null) {
                outList.add(cachedSong)
                continue
            }

            // 2. Fast parse without opening MediaMetadataRetriever
            val audioBaseName = audioDoc.displayName.substringBeforeLast('.').lowercase(Locale.ROOT)
            val matchedJpg = jpgDocs.firstOrNull { jpg ->
                jpg.displayName.substringBeforeLast('.').equals(audioBaseName, ignoreCase = true)
            } ?: jpgDocs.firstOrNull { jpg ->
                val lower = jpg.displayName.lowercase(Locale.ROOT)
                STANDARD_ART_NAMES.any { lower.contains(it) }
            } ?: jpgDocs.firstOrNull()

            val ext = audioDoc.displayName.substringAfterLast('.', "").uppercase(Locale.ROOT)
            val format = when (ext) {
                "FLAC" -> "FLAC"
                "WAV" -> "WAV"
                "AAC" -> "AAC"
                "OGG" -> "OGG"
                "M4A" -> "M4A"
                "OPUS" -> "OPUS"
                else -> "MP3"
            }
            val isHiRes = format == "FLAC" || format == "WAV"

            val (parsedArtist, parsedTitle) = fastParseTrackDetails(audioDoc.displayName, folderName)

            // Fast duration calculation from file size
            val estimatedDurationMs = if (audioDoc.size > 0) {
                val bitRate = if (isHiRes) 1411_000L else 320_000L
                ((audioDoc.size * 8L * 1000L) / bitRate).coerceIn(30_000L, 1_800_000L)
            } else {
                180_000L
            }

            val fastSong = Song(
                id = uriStr,
                title = parsedTitle,
                artist = parsedArtist,
                album = folderName,
                durationMs = estimatedDurationMs,
                uriString = uriStr,
                format = format,
                isHiRes = isHiRes,
                albumArtUriString = matchedJpg?.uri?.toString(),
                folderName = folderName,
                bitrateKbps = if (isHiRes) 1411 else 320,
                sampleRateHz = if (isHiRes) 48000 else 44100,
                isUsb = isUsbSource
            )
            outList.add(fastSong)
        }
    }

    /**
     * Fast scan of device MediaStore with Room cache acceleration.
     */
    suspend fun scanDeviceMediaStore(context: Context, useCache: Boolean = true): List<Song> = withContext(Dispatchers.IO) {
        val repo = MusicCacheRepository(context)
        if (useCache) {
            try {
                val cached = repo.getSongsBySourceType("MEDIASTORE")
                if (cached.isNotEmpty()) {
                    return@withContext cached
                }
            } catch (e: Exception) {
                // fallback to live scan
            }
        }

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

                    // Fast resolve companion art
                    var artUriString: String? = null
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

                    if (artUriString == null && albumIdCol >= 0) {
                        val albumId = it.getLong(albumIdCol)
                        if (albumId > 0) {
                            artUriString = ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            ).toString()
                        }
                    }

                    songs.add(
                        Song(
                            id = "mediastore_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = if (duration > 0) duration else 180000L,
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

            // Cache MediaStore tracks in Room database
            if (songs.isNotEmpty()) {
                try {
                    repo.saveScannedSongs(songs, folderUri = "mediastore", sourceType = "MEDIASTORE")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songs
    }

    /**
     * Lazy enrichment of metadata (ID3 tags) for a specific song when played or background synced.
     * Persists updated metadata directly to Room database cache.
     */
    suspend fun enrichMetadata(context: Context, song: Song): Song = withContext(Dispatchers.IO) {
        if (song.isBuiltIn || song.uriString.startsWith("content://media/external/audio/")) {
            return@withContext song
        }

        val retriever = MediaMetadataRetriever()
        var updatedTitle = song.title
        var updatedArtist = song.artist
        var updatedAlbum = song.album
        var updatedDuration = song.durationMs
        var updatedBitrate = song.bitrateKbps
        var artUriStr = song.albumArtUriString

        try {
            retriever.setDataSource(context, Uri.parse(song.uriString))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                if (it.isNotBlank()) updatedTitle = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                if (it.isNotBlank()) updatedArtist = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                if (it.isNotBlank()) updatedAlbum = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                if (it > 0) updatedDuration = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let {
                if (it > 0) updatedBitrate = it / 1000
            }

            if (artUriStr == null) {
                val picture = retriever.embeddedPicture
                if (picture != null) {
                    val artFile = File(context.cacheDir, "art_${abs(song.uriString.hashCode())}.jpg")
                    if (!artFile.exists() || artFile.length() == 0L) {
                        FileOutputStream(artFile).use { it.write(picture) }
                    }
                    artUriStr = Uri.fromFile(artFile).toString()
                }
            }
        } catch (e: Exception) {
            // keep existing fast values
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }

        val enrichedSong = song.copy(
            title = updatedTitle,
            artist = updatedArtist,
            album = updatedAlbum,
            durationMs = updatedDuration,
            bitrateKbps = updatedBitrate,
            albumArtUriString = artUriStr
        )

        // Persist enriched tags in Room database
        try {
            MusicCacheRepository(context).updateSong(enrichedSong)
        } catch (e: Exception) {
            // keep memory values
        }

        enrichedSong
    }
}
