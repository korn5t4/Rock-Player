package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.audio.RockAudioEngine
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import java.util.Locale

/**
 * 4x2 Home Screen Widget for Rock Player: Full vintage studio deck with large album art,
 * track specs, progress scrubber, and full 5-button transport controls.
 */
class RockPlayerWidget4x2Provider : AppWidgetProvider() {

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
            ACTION_SHUFFLE -> {
                engine.toggleShuffle()
                updateAllWidgets(context)
            }
            ACTION_REPEAT -> {
                engine.cycleRepeatMode()
                updateAllWidgets(context)
            }
            ACTION_WIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.rockplayer.widget4x2.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.rockplayer.widget4x2.ACTION_NEXT"
        const val ACTION_PREV = "com.example.rockplayer.widget4x2.ACTION_PREV"
        const val ACTION_SHUFFLE = "com.example.rockplayer.widget4x2.ACTION_SHUFFLE"
        const val ACTION_REPEAT = "com.example.rockplayer.widget4x2.ACTION_REPEAT"
        const val ACTION_WIDGET_UPDATE = "com.example.rockplayer.widget4x2.ACTION_WIDGET_UPDATE"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val thisWidget = ComponentName(context, RockPlayerWidget4x2Provider::class.java)
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
                val isShuffle = engine.isShuffle.value
                val repeatMode = engine.repeatMode.value
                val currentPreset = engine.currentPreset.value

                val views = RemoteViews(context.packageName, R.layout.widget_rock_player_4x2)

                // 1. Tapping main body/album art opens MainActivity into the Player
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("target_screen", "PLAYER")
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    200,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_4x2_root, openAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_4x2_album_container, openAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_4x2_info_container, openAppPendingIntent)

                // 2. Tapping Equalizer button badge opens MainActivity directly into Equalizer!
                val openEqIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("target_screen", "EQUALIZER")
                }
                val openEqPending = PendingIntent.getActivity(
                    context,
                    201,
                    openEqIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_4x2_btn_eq, openEqPending)

                // 3. Play / Pause Button Action
                val playPauseIntent = Intent(context, RockPlayerWidget4x2Provider::class.java).apply {
                    action = ACTION_PLAY_PAUSE
                }
                val playPausePending = PendingIntent.getBroadcast(
                    context,
                    202,
                    playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_4x2_btn_play_pause, playPausePending)

                // 4. Next Button Action
                val nextIntent = Intent(context, RockPlayerWidget4x2Provider::class.java).apply {
                    action = ACTION_NEXT
                }
                val nextPending = PendingIntent.getBroadcast(
                    context,
                    203,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_4x2_btn_next, nextPending)

                // 5. Previous Button Action
                val prevIntent = Intent(context, RockPlayerWidget4x2Provider::class.java).apply {
                    action = ACTION_PREV
                }
                val prevPending = PendingIntent.getBroadcast(
                    context,
                    204,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_4x2_btn_prev, prevPending)

                // 6. Shuffle Button Action
                val shuffleIntent = Intent(context, RockPlayerWidget4x2Provider::class.java).apply {
                    action = ACTION_SHUFFLE
                }
                val shufflePending = PendingIntent.getBroadcast(
                    context,
                    205,
                    shuffleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_4x2_btn_shuffle, shufflePending)

                // 7. Repeat Button Action
                val repeatIntent = Intent(context, RockPlayerWidget4x2Provider::class.java).apply {
                    action = ACTION_REPEAT
                }
                val repeatPending = PendingIntent.getBroadcast(
                    context,
                    206,
                    repeatIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_4x2_btn_repeat, repeatPending)

                // 8. Update Play/Pause Icon State
                views.setImageViewResource(
                    R.id.widget_4x2_btn_play_pause,
                    if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                )

                // 9. Update Shuffle and Repeat Visual Indicators
                views.setInt(
                    R.id.widget_4x2_btn_shuffle,
                    "setColorFilter",
                    if (isShuffle) Color.parseColor("#00E5FF") else Color.parseColor("#8E97A8")
                )

                val (repeatIcon, repeatColor) = when (repeatMode) {
                    RepeatMode.ONE -> Pair(R.drawable.ic_widget_repeat_one, Color.parseColor("#FFD700"))
                    RepeatMode.ALL -> Pair(R.drawable.ic_widget_repeat, Color.parseColor("#00E5FF"))
                    RepeatMode.OFF -> Pair(R.drawable.ic_widget_repeat, Color.parseColor("#8E97A8"))
                }
                views.setImageViewResource(R.id.widget_4x2_btn_repeat, repeatIcon)
                views.setInt(R.id.widget_4x2_btn_repeat, "setColorFilter", repeatColor)

                // 10. Update Equalizer Preset Badge
                views.setTextViewText(R.id.widget_4x2_eq_label, "EQ: ${currentPreset.uppercase(Locale.ROOT)}")

                // 11. Update Track Details
                if (currentSong != null) {
                    views.setTextViewText(R.id.widget_4x2_track_title, currentSong.title)
                    views.setTextViewText(R.id.widget_4x2_track_artist, currentSong.artist)

                    val formatLabel = if (currentSong.isHiRes) "HI-RES FLAC" else currentSong.format
                    views.setTextViewText(R.id.widget_4x2_format_badge, formatLabel)

                    val specs = "${currentSong.album} • ${currentSong.bitrateKbps} kbps"
                    views.setTextViewText(R.id.widget_4x2_audio_specs, specs)

                    val progress = if (durationMs > 0) {
                        ((positionMs * 100) / durationMs).toInt().coerceIn(0, 100)
                    } else 0
                    views.setProgressBar(R.id.widget_4x2_progress_bar, 100, progress, false)

                    views.setTextViewText(R.id.widget_4x2_time_elapsed, formatTime(positionMs))
                    views.setTextViewText(R.id.widget_4x2_time_total, formatTime(durationMs))

                    val artBitmap = loadAlbumArt(context, currentSong)
                    if (artBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_4x2_album_art, artBitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_4x2_album_art, R.drawable.cover_electric_thunder)
                    }
                } else {
                    views.setTextViewText(R.id.widget_4x2_track_title, "Rock Player")
                    views.setTextViewText(R.id.widget_4x2_track_artist, "Tap to start playback")
                    views.setTextViewText(R.id.widget_4x2_format_badge, "HI-RES")
                    views.setTextViewText(R.id.widget_4x2_audio_specs, "Studio Deck • 24-bit / 96kHz")
                    views.setProgressBar(R.id.widget_4x2_progress_bar, 100, 0, false)
                    views.setTextViewText(R.id.widget_4x2_time_elapsed, "00:00")
                    views.setTextViewText(R.id.widget_4x2_time_total, "00:00")
                    views.setImageViewResource(R.id.widget_4x2_album_art, R.drawable.cover_electric_thunder)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun formatTime(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

        private fun loadAlbumArt(context: Context, song: Song): Bitmap? {
            val cachedFile = com.example.data.image.AlbumArtDiskCache.getCachedArtworkFile(context, song.id)
            if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val original = BitmapFactory.decodeFile(cachedFile.absolutePath, opts)
                    if (original != null) {
                        return Bitmap.createScaledBitmap(original, 160, 160, true)
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
                            Bitmap.createScaledBitmap(original, 160, 160, true)
                        } else null
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
