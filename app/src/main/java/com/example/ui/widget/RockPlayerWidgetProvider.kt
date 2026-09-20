package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.audio.RockAudioEngine
import com.example.data.model.Song

class RockPlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val engine = RockAudioEngine.getInstance(context)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                engine.togglePlayPause()
                updateAllWidgets(context)
            }
            ACTION_NEXT -> {
                engine.playNext()
                updateAllWidgets(context)
            }
            ACTION_PREV -> {
                engine.playPrevious()
                updateAllWidgets(context)
            }
            ACTION_WIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.rockplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.rockplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.example.rockplayer.ACTION_PREV"
        const val ACTION_WIDGET_UPDATE = "com.example.rockplayer.ACTION_WIDGET_UPDATE"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val thisWidget = ComponentName(context, RockPlayerWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val engine = RockAudioEngine.getInstance(context)
                val currentSong = engine.currentSong.value
                val isPlaying = engine.isPlaying.value
                val positionMs = engine.currentPositionMs.value
                val durationMs = engine.durationMs.value

                val views = RemoteViews(context.packageName, R.layout.widget_rock_player)

                // 1. Tapping widget body opens MainActivity into the player
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_album_container, openAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_info_container, openAppPendingIntent)

                // 2. Play / Pause Button Action
                val playPauseIntent = Intent(context, RockPlayerWidgetProvider::class.java).apply {
                    action = ACTION_PLAY_PAUSE
                }
                val playPausePending = PendingIntent.getBroadcast(
                    context,
                    101,
                    playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePending)

                // 3. Next Button Action
                val nextIntent = Intent(context, RockPlayerWidgetProvider::class.java).apply {
                    action = ACTION_NEXT
                }
                val nextPending = PendingIntent.getBroadcast(
                    context,
                    102,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

                // 4. Previous Button Action
                val prevIntent = Intent(context, RockPlayerWidgetProvider::class.java).apply {
                    action = ACTION_PREV
                }
                val prevPending = PendingIntent.getBroadcast(
                    context,
                    103,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

                // 5. Update Play/Pause Icon State
                views.setImageViewResource(
                    R.id.widget_btn_play_pause,
                    if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                )

                // 6. Update Track Details
                if (currentSong != null) {
                    views.setTextViewText(R.id.widget_track_title, currentSong.title)
                    views.setTextViewText(R.id.widget_track_artist, currentSong.artist)
                    views.setTextViewText(
                        R.id.widget_format_badge,
                        if (currentSong.isHiRes) "HI-RES" else currentSong.format
                    )

                    val progress = if (durationMs > 0) {
                        ((positionMs * 100) / durationMs).toInt().coerceIn(0, 100)
                    } else 0
                    views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)

                    val artBitmap = loadAlbumArt(context, currentSong)
                    if (artBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_album_art, artBitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_album_art, R.drawable.cover_electric_thunder)
                    }
                } else {
                    views.setTextViewText(R.id.widget_track_title, "Rock Player")
                    views.setTextViewText(R.id.widget_track_artist, "Tap to start playback")
                    views.setTextViewText(R.id.widget_format_badge, "HI-RES")
                    views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.cover_electric_thunder)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun loadAlbumArt(context: Context, song: Song): Bitmap? {
            // 1. Fast path: check local disk cache file directly (<1ms, no ContentResolver IPC)
            val cachedFile = com.example.data.image.AlbumArtDiskCache.getCachedArtworkFile(context, song.id)
            if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val original = BitmapFactory.decodeFile(cachedFile.absolutePath, opts)
                    if (original != null) {
                        return Bitmap.createScaledBitmap(original, 120, 120, true)
                    }
                } catch (e: Exception) {
                    // Fall back
                }
            }

            val uriStr = song.albumArtUriString ?: return null
            return try {
                val uri = Uri.parse(uriStr)
                if (uri.scheme == "content" || uri.scheme == "file") {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        val original = BitmapFactory.decodeStream(stream, null, opts)
                        if (original != null) {
                            Bitmap.createScaledBitmap(original, 120, 120, true)
                        } else null
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
