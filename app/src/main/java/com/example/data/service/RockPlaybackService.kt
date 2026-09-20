package com.example.data.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.audio.RockAudioEngine
import com.example.data.image.AlbumArtDiskCache
import com.example.data.model.Song
import com.example.data.preferences.PlayerPreferences
import com.example.ui.lockscreen.LockScreenPlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground Service responsible for:
 * 1. Keeping audio playback alive through device lock & screen-off sleep.
 * 2. Publishing an interactive MediaStyle Notification with VISIBILITY_PUBLIC
 *    which Android surfaces natively as a mini player on the device lock screen.
 * 3. Detecting screen wake-up while locked to automatically surface the custom
 *    Rock Lock Screen Mini Player (LockScreenPlayerActivity).
 */
class RockPlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var lastObservedSongId: String? = null

    companion object {
        const val CHANNEL_ID = "rock_player_playback_channel"
        const val NOTIFICATION_ID = 7701

        const val ACTION_PLAY_PAUSE = "com.example.rockplayer.service.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.rockplayer.service.NEXT"
        const val ACTION_PREV = "com.example.rockplayer.service.PREV"
        const val ACTION_STOP = "com.example.rockplayer.service.STOP"
        const val ACTION_UPDATE = "com.example.rockplayer.service.UPDATE"
        const val ACTION_OPEN_LOCKSCREEN = "com.example.rockplayer.service.OPEN_LOCKSCREEN"

        fun startOrUpdate(context: Context) {
            try {
                val intent = Intent(context, RockPlaybackService::class.java).apply {
                    action = ACTION_UPDATE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Ignore background start exceptions if app process is shutting down
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, RockPlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        setupMediaSession()
        registerScreenReceiver()
        observeAudioEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = RockAudioEngine.getInstance(applicationContext)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                engine.togglePlayPause()
            }
            ACTION_NEXT -> {
                engine.playNext()
            }
            ACTION_PREV -> {
                engine.playPrevious()
            }
            ACTION_STOP -> {
                if (engine.isPlaying.value) {
                    engine.togglePlayPause()
                }
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_OPEN_LOCKSCREEN -> {
                LockScreenPlayerActivity.launch(applicationContext)
            }
        }

        syncNotificationAndSession()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rock Player Lock Screen Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mini player and playback controls visible while device is locked"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "RockPlayerMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    RockAudioEngine.getInstance(applicationContext).togglePlayPause()
                }

                override fun onPause() {
                    RockAudioEngine.getInstance(applicationContext).togglePlayPause()
                }

                override fun onSkipToNext() {
                    RockAudioEngine.getInstance(applicationContext).playNext()
                }

                override fun onSkipToPrevious() {
                    RockAudioEngine.getInstance(applicationContext).playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    RockAudioEngine.getInstance(applicationContext).seekTo(pos)
                }

                override fun onStop() {
                    val engine = RockAudioEngine.getInstance(applicationContext)
                    if (engine.isPlaying.value) engine.togglePlayPause()
                    stopForeground(true)
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    private fun registerScreenReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_ON) {
                    val engine = RockAudioEngine.getInstance(applicationContext)
                    val prefs = PlayerPreferences(applicationContext)
                    if (engine.isPlaying.value && prefs.lockScreenPlayerEnabled) {
                        val keyguardManager = getSystemService(KeyguardManager::class.java)
                        if (keyguardManager != null && keyguardManager.isKeyguardLocked) {
                            LockScreenPlayerActivity.launch(applicationContext)
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun observeAudioEngine() {
        val engine = RockAudioEngine.getInstance(applicationContext)

        // Observe song changes
        serviceScope.launch {
            engine.currentSong.collectLatest { song ->
                if (song != null && song.id != lastObservedSongId) {
                    lastObservedSongId = song.id
                    val prefs = PlayerPreferences(applicationContext)
                    if (prefs.wakeScreenOnTrackChange && engine.isPlaying.value) {
                        wakeScreenBriefly()
                    }
                }
                syncNotificationAndSession()
            }
        }

        // Observe playback state changes
        serviceScope.launch {
            engine.isPlaying.collectLatest {
                syncNotificationAndSession()
            }
        }
    }

    private fun wakeScreenBriefly() {
        try {
            val pm = getSystemService(PowerManager::class.java)
            @Suppress("DEPRECATION")
            val wakeLock = pm?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "RockPlayer:LockScreenWake"
            )
            wakeLock?.acquire(3000L)
        } catch (e: Exception) {}
    }

    private fun syncNotificationAndSession() {
        serviceScope.launch {
            val engine = RockAudioEngine.getInstance(applicationContext)
            val song = engine.currentSong.value
            val isPlaying = engine.isPlaying.value
            val currentPos = engine.currentPositionMs.value
            val duration = engine.durationMs.value

            if (song == null) {
                if (!isPlaying) {
                    stopForeground(false)
                }
                return@launch
            }

            // 1. Update MediaSession PlaybackState
            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val playbackState = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP
                )
                .setState(state, currentPos, 1.0f)
                .build()
            mediaSession?.setPlaybackState(playbackState)

            // 2. Load artwork for lock screen system display
            val artBitmap = withContext(Dispatchers.IO) {
                loadAlbumArtBitmap(song)
            }

            // 3. Update MediaMetadata
            val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)

            if (artBitmap != null) {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artBitmap)
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, artBitmap)
            }
            mediaSession?.setMetadata(metadataBuilder.build())

            // 4. Build Notification
            val notification = buildLockScreenNotification(song, isPlaying, artBitmap)

            // 5. Post notification / foreground state
            try {
                if (isPlaying) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    stopForeground(false)
                    notificationManager?.notify(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                try {
                    notificationManager?.notify(NOTIFICATION_ID, notification)
                } catch (e2: Exception) {}
            }
        }
    }

    private fun loadAlbumArtBitmap(song: Song): Bitmap? {
        // Fast path: cached file in Coil disk cache
        val cached = AlbumArtDiskCache.getCachedArtworkFile(applicationContext, song.id)
        if (cached != null && cached.exists() && cached.length() > 0) {
            try {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bmp = BitmapFactory.decodeFile(cached.absolutePath, opts)
                if (bmp != null) return Bitmap.createScaledBitmap(bmp, 256, 256, true)
            } catch (e: Exception) {}
        }

        // Fallback to Uri
        val uriStr = song.albumArtUriString ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val raw = BitmapFactory.decodeStream(stream, null, opts)
                if (raw != null) Bitmap.createScaledBitmap(raw, 256, 256, true) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildLockScreenNotification(
        song: Song,
        isPlaying: Boolean,
        artBitmap: Bitmap?
    ): Notification {
        // PendingIntents for playback controls
        val prevPending = PendingIntent.getService(
            this, 1,
            Intent(this, RockPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePending = PendingIntent.getService(
            this, 2,
            Intent(this, RockPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPending = PendingIntent.getService(
            this, 3,
            Intent(this, RockPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent: tapping opens the Lock Screen Mini Player if device is locked,
        // or MainActivity if unlocked.
        val lockScreenIntent = Intent(this, LockScreenPlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this, 4, lockScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder
            .setContentTitle(song.title)
            .setContentText("${song.artist} • ${song.album}")
            .setSubText("Rock Player")
            .setSmallIcon(R.drawable.ic_rock_hand)
            .setContentIntent(contentPending)
            // CRITICAL: VISIBILITY_PUBLIC ensures the notification is fully visible on the lock screen!
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    prevPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    playPauseIcon,
                    playPauseTitle,
                    playPausePending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Next",
                    nextPending
                ).build()
            )

        if (artBitmap != null) {
            builder.setLargeIcon(artBitmap)
        }

        // Apply MediaStyle
        val sessionToken = mediaSession?.sessionToken
        if (sessionToken != null) {
            val mediaStyle = Notification.MediaStyle()
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
            builder.setStyle(mediaStyle)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
        mediaSession?.release()
        mediaSession = null
    }
}
