package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import com.example.data.image.AlbumArtDiskCache
import com.example.data.image.AlbumArtDiskCacheInterceptor
import com.example.data.image.SongAlbumArtFetcher
import com.example.data.image.SongKeyer

class RockPlayerApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val diskCache = AlbumArtDiskCache.createDiskCache(this)
        val memoryCache = AlbumArtDiskCache.createMemoryCache(this)

        return ImageLoader.Builder(this)
            .diskCache(diskCache)
            .memoryCache(memoryCache)
            .components {
                add(SongKeyer())
                add(SongAlbumArtFetcher.Factory(this@RockPlayerApplication))
                add(AlbumArtDiskCacheInterceptor(this@RockPlayerApplication, diskCache))
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(true)
            .build()
    }
}
