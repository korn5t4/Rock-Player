package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.R
import com.example.data.image.AlbumArtDiskCache
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs

/**
 * Universal high-performance Album Art Resolver supporting all picture formats and extensions:
 * - Standard web & raster: JPG, JPEG, JPE, JFIF, PNG, WEBP, GIF, BMP, DIB
 * - High Efficiency & Next-Gen: HEIC, HEIF, HIF, AVIF, AVIS, JP2, J2K, JPF, JPM, JXL
 * - Icons & Vector graphics: ICO, CUR, SVG, SVGZ
 * - Pro, Textures & HDR: TIFF, TIF, PSD, TGA, DDS, WBMP, HDR, EXR
 * - Digital Camera RAW formats: RAW, ARW, CR2, CR3, NEF, NRW, DNG, ORF, RW2, PEF, RAF, SRW
 * - Netpbm & Bitmaps: PBM, PGM, PPM, PNM, PAM, XBM, XPM
 */
object AlbumArtResolver {

    private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, Uri>()

    val STANDARD_ART_NAMES = listOf(
        "cover", "folder", "album", "albumart", "front", "artwork", "art",
        "cd", "disc", "back", "insert", "sleeve", "poster", "fanart"
    )

    /**
     * Prioritized picture extensions for rapid companion image discovery.
     */
    val PRIORITY_PICTURE_EXTENSIONS: List<String> = listOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif",
        "tiff", "tif", "svg", "ico", "jp2", "jxl", "tga", "dds", "wbmp", "psd"
    )

    /**
     * Complete, exhaustive set of all supported picture formats and extensions for album art.
     */
    val ALL_PICTURE_EXTENSIONS: Set<String> = setOf(
        // Standard JPEG variations
        "jpg", "jpeg", "jpe", "jif", "jfif", "jfi",
        // Portable Network Graphics
        "png",
        // Modern Web formats
        "webp",
        // Graphics Interchange Format
        "gif",
        // Bitmaps
        "bmp", "dib",
        // High Efficiency formats
        "heic", "heif", "hif",
        // AV1 Image File Format
        "avif", "avis",
        // JPEG 2000
        "jp2", "j2k", "jpf", "jpm", "jpg2", "j2c",
        // JPEG XL
        "jxl",
        // Icons and Cursors
        "ico", "cur",
        // Vector Graphics
        "svg", "svgz",
        // Tagged Image File Format
        "tiff", "tif",
        // Design, Textures & Games
        "tga", "icb", "vda", "vst", "dds", "wbmp", "psd",
        // High Dynamic Range
        "hdr", "exr",
        // Digital Camera RAW
        "raw", "arw", "cr2", "cr3", "nef", "nrw", "dng", "orf", "rw2", "pef", "raf", "srw",
        // Netpbm & X Bitmaps
        "pbm", "pgm", "ppm", "pnm", "pam", "xbm", "xpm"
    )

    /**
     * Returns true if the file name or MIME type represents a supported picture/image format.
     */
    fun isPictureFile(fileName: String, mimeType: String? = null): Boolean {
        if (mimeType != null && mimeType.startsWith("image/")) return true
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ALL_PICTURE_EXTENSIONS.contains(ext)
    }

    /**
     * Maps any picture extension to its precise MIME type for content loaders and Coil decoders.
     */
    fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "jpg", "jpeg", "jpe", "jif", "jfif", "jfi" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp", "dib" -> "image/bmp"
            "heic", "heif", "hif" -> "image/heif"
            "avif", "avis" -> "image/avif"
            "svg", "svgz" -> "image/svg+xml"
            "tiff", "tif" -> "image/tiff"
            "ico", "cur" -> "image/x-icon"
            "jp2", "j2k", "jpf", "jpm", "jpg2", "j2c" -> "image/jp2"
            "jxl" -> "image/jxl"
            "tga", "icb", "vda", "vst" -> "image/x-tga"
            "dds" -> "image/vnd-ms.dds"
            "wbmp" -> "image/vnd.wap.wbmp"
            "psd" -> "image/vnd.adobe.photoshop"
            "hdr" -> "image/vnd.radiance"
            "exr" -> "image/x-exr"
            "dng" -> "image/x-adobe-dng"
            "cr2", "cr3" -> "image/x-canon-cr2"
            "nef", "nrw" -> "image/x-nikon-nef"
            "arw" -> "image/x-sony-arw"
            "orf" -> "image/x-olympus-orf"
            "rw2" -> "image/x-panasonic-rw2"
            "pef" -> "image/x-pentax-pef"
            "raf" -> "image/x-fuji-raf"
            "srw" -> "image/x-samsung-srw"
            "raw" -> "image/raw"
            "pbm" -> "image/x-portable-bitmap"
            "pgm" -> "image/x-portable-graymap"
            "ppm" -> "image/x-portable-pixmap"
            "pnm" -> "image/x-portable-anymap"
            "xbm" -> "image/x-xbitmap"
            "xpm" -> "image/x-xpixmap"
            else -> "image/jpeg"
        }
    }

    /**
     * Resolves the album artwork URI for the given song across all picture extensions.
     * Looks for:
     * 1. In-memory cache
     * 2. Existing valid albumArtUri on the song
     * 3. Companion picture of ANY extension in the same directory (e.g. song.png, cover.jpg, folder.webp, art.gif, etc.)
     * 4. Embedded album art extracted from the audio file
     * 5. MediaStore album art
     * 6. Built-in rock album cover for demo tracks
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

        // 0. Check fast local disk cache
        AlbumArtDiskCache.getCachedArtworkFile(context, song.id)?.let { cachedFile ->
            return Uri.fromFile(cachedFile)
        }

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

        // 2. Check for companion picture of any supported extension in the file's folder (direct filesystem)
        try {
            val audioUri = Uri.parse(song.uriString)
            if (audioUri.scheme == "file") {
                val audioFile = File(audioUri.path ?: "")
                findCompanionPicture(audioFile)?.let { picFile ->
                    return Uri.fromFile(picFile)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 3. If MediaStore song, check DATA column for disk path and companion picture, or album art URI
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
                                findCompanionPicture(audioFile)?.let { picFile ->
                                    return Uri.fromFile(picFile)
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

        // 4. Extract embedded picture via MediaMetadataRetriever and cache on disk
        try {
            val audioUri = Uri.parse(song.uriString)
            val embeddedFile = extractEmbeddedArt(context, audioUri, "art_${abs(song.id.hashCode())}")
            if (embeddedFile != null) {
                return Uri.fromFile(embeddedFile)
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
     * Searches the directory containing [audioFile] for companion picture files of ANY supported extension:
     * 1. Exact match with same base name: e.g. <audio_name>.jpg, <audio_name>.png, <audio_name>.webp, etc.
     * 2. Standard album names: cover.*, folder.*, album.*, artwork.*, front.*, art.*, etc.
     * 3. Files in directory whose name starts with base name or contains standard album keywords.
     * 4. Any picture file located in that directory.
     */
    fun findCompanionPicture(audioFile: File): File? {
        if (!audioFile.exists()) return null
        val parentDir = audioFile.parentFile ?: return null
        if (!parentDir.exists() || !parentDir.isDirectory) return null

        val baseName = audioFile.nameWithoutExtension.lowercase(Locale.ROOT)

        // 1. Same filename with priority picture extensions first
        for (ext in PRIORITY_PICTURE_EXTENSIONS) {
            val candidate = File(parentDir, "${audioFile.nameWithoutExtension}.$ext")
            if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                return candidate
            }
        }
        // 1b. Remaining picture extensions with same filename
        for (ext in ALL_PICTURE_EXTENSIONS) {
            if (PRIORITY_PICTURE_EXTENSIONS.contains(ext)) continue
            val candidate = File(parentDir, "${audioFile.nameWithoutExtension}.$ext")
            if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                return candidate
            }
        }

        // 2. Standard album names (cover.*, folder.*, album.*, etc.)
        for (stdName in STANDARD_ART_NAMES) {
            for (ext in PRIORITY_PICTURE_EXTENSIONS) {
                val candidate = File(parentDir, "$stdName.$ext")
                if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                    return candidate
                }
            }
        }
        for (stdName in STANDARD_ART_NAMES) {
            for (ext in ALL_PICTURE_EXTENSIONS) {
                if (PRIORITY_PICTURE_EXTENSIONS.contains(ext)) continue
                val candidate = File(parentDir, "$stdName.$ext")
                if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                    return candidate
                }
            }
        }

        // 3. Scan directory files for any matching picture file
        try {
            val files = parentDir.listFiles() ?: return null

            // Prioritize picture files whose name starts with audio base name or contains standard art keywords
            val bestMatch = files.firstOrNull { f ->
                if (!f.isFile || f.length() == 0L) return@firstOrNull false
                val lower = f.name.lowercase(Locale.ROOT)
                val isPic = isPictureFile(lower)
                isPic && (lower.startsWith(baseName) || STANDARD_ART_NAMES.any { lower.contains(it) })
            }
            if (bestMatch != null) return bestMatch

            // Otherwise, any picture file found in that folder
            val anyPic = files.firstOrNull { f ->
                f.isFile && f.length() > 0 && isPictureFile(f.name)
            }
            if (anyPic != null) return anyPic
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    /**
     * Backward-compatible synonym for [findCompanionPicture], searching all picture extensions.
     */
    fun findCompanionJpg(audioFile: File): File? = findCompanionPicture(audioFile)

    /**
     * Extracts embedded picture from audio URI and writes it to a file with proper extension in cache.
     */
    fun extractEmbeddedArt(context: Context, audioUri: Uri, baseName: String): File? {
        // Check if any previously cached artwork file already exists for this baseName
        for (ext in listOf("jpg", "png", "webp", "gif", "bmp")) {
            val candidate = File(context.cacheDir, "$baseName.$ext")
            if (candidate.exists() && candidate.length() > 0) {
                return candidate
            }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri)
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                val ext = detectPictureExtension(picture)
                val targetFile = File(context.cacheDir, "$baseName.$ext")
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

    /**
     * Backward-compatible embedded art extractor.
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

    /**
     * Inspects magic header bytes of image data to determine proper file extension.
     */
    private fun detectPictureExtension(bytes: ByteArray): String {
        if (bytes.size < 4) return "jpg"
        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "jpg"
        }
        // PNG: 89 50 4E 47
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
            return "png"
        }
        // GIF: 47 49 46 38
        if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()) {
            return "gif"
        }
        // BMP: 42 4D
        if (bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()) {
            return "bmp"
        }
        // WebP: RIFF ... WEBP
        if (bytes.size >= 12 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return "webp"
        }
        return "jpg"
    }
}
