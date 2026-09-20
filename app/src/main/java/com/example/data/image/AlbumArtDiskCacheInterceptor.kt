package com.example.data.image

import android.content.Context
import android.net.Uri
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.request.ImageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.buffer
import okio.source
import kotlin.math.abs

/**
 * Interceptor that intercepts content:// and android.resource:// album art URIs
 * and ensures they are mirrored into Coil's fast local DiskCache.
 *
 * This eliminates repeated ContentResolver IPC transactions and Storage Access Framework
 * binder calls during list scrolling, saving considerable battery and keeping the UI thread fluid.
 */
class AlbumArtDiskCacheInterceptor(
    private val context: Context,
    private val diskCache: DiskCache
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult = withContext(Dispatchers.IO) {
        val request = chain.request
        val data = request.data

        if (data is Uri) {
            val scheme = data.scheme
            if (scheme == "content" || scheme == "android.resource") {
                val cacheKey = "album_art_uri_${abs(data.toString().hashCode())}"

                // 1. Check if we already have this content URI mirrored in disk cache
                diskCache.openSnapshot(cacheKey)?.use { snapshot ->
                    val localFile = snapshot.data.toFile()
                    if (localFile.exists() && localFile.length() > 0) {
                        val redirectedRequest = request.newBuilder()
                            .data(localFile)
                            .build()
                        return@withContext chain.proceed(redirectedRequest)
                    }
                }

                // 2. Not cached yet: read the input stream and cache to disk
                val editor = diskCache.openEditor(cacheKey)
                if (editor != null) {
                    try {
                        context.contentResolver.openInputStream(data)?.use { input ->
                            FileSystem.SYSTEM.sink(editor.data).buffer().use { sink ->
                                sink.writeAll(input.source())
                            }
                        }
                        val committedSnapshot = editor.commitAndOpenSnapshot()
                        if (committedSnapshot != null) {
                            val localFile = committedSnapshot.data.toFile()
                            committedSnapshot.close()
                            if (localFile.exists() && localFile.length() > 0) {
                                val redirectedRequest = request.newBuilder()
                                    .data(localFile)
                                    .build()
                                return@withContext chain.proceed(redirectedRequest)
                            }
                        }
                    } catch (e: Exception) {
                        try {
                            editor.abort()
                        } catch (abortEx: Exception) {
                            // Ignore
                        }
                    }
                }
            }
        }

        chain.proceed(request)
    }
}
