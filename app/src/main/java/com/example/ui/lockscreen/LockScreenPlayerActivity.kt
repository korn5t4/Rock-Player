package com.example.ui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.audio.RockAudioEngine
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.preferences.PlayerPreferences
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PlayerSkinTheme
import com.example.ui.theme.PlayerSkins
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-visibility Lock Screen Mini Player Activity.
 * Rendered above the Android device keyguard (setShowWhenLocked / turnScreenOn)
 * to provide a rich, responsive rock player interface when the device is locked.
 */
class LockScreenPlayerActivity : ComponentActivity() {

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, LockScreenPlayerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenVisibility()
        enableEdgeToEdge()

        val engine = RockAudioEngine.getInstance(applicationContext)
        val prefs = PlayerPreferences(applicationContext)
        val currentSkin = PlayerSkins.getSkinById(prefs.selectedThemeId)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                LockScreenMiniPlayerContent(
                    engine = engine,
                    skin = currentSkin,
                    onUnlockAndOpenApp = {
                        requestUnlockAndOpenApp()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }

    private fun configureLockScreenVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun requestUnlockAndOpenApp() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguardManager != null && keyguardManager.isKeyguardLocked) {
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    openMainActivity()
                }

                override fun onDismissError() {
                    openMainActivity()
                }
            })
        } else {
            openMainActivity()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun LockScreenMiniPlayerContent(
    engine: RockAudioEngine,
    skin: PlayerSkinTheme,
    onUnlockAndOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentSong by engine.currentSong.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()
    val currentPosMs by engine.currentPositionMs.collectAsState()
    val durationMs by engine.durationMs.collectAsState()
    val repeatMode by engine.repeatMode.collectAsState()
    val isShuffle by engine.isShuffle.collectAsState()
    val visualizerBars by engine.visualizerBars.collectAsState()

    // Clock state
    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeStr = "${timeFormat.format(now)} ${amPmFormat.format(now).uppercase()}"
            currentDateStr = dateFormat.format(now)
            delay(1000L)
        }
    }

    // Vinyl spin animation when playing
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 8000, easing = LinearEasing),
                    repeatMode = AnimRepeatMode.Restart
                )
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lock_screen_mini_player_surface"),
        color = Color(0xEB0A0A0E) // Translucent deep midnight lock screen overlay
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. TOP CLOCK & LOCK STATUS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Device Locked",
                            tint = skin.albumFrameColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LOCKED • ROCK MINI PLAYER",
                            color = skin.textCyanColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_lockscreen_dismiss")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Digital Clock
                Text(
                    text = currentTimeStr,
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = currentDateStr,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. THE RETRO ROCK FLOATING MINI PLAYER CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 20.dp, shape = RoundedCornerShape(20.dp))
                        .testTag("lock_screen_player_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = skin.surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(2.dp, skin.cardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Song Cover + Details Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Album Art Square with yellow matting frame
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(skin.albumFrameColor)
                                    .border(2.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF101014)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val context = LocalContext.current
                                    val albumModel = currentSong?.albumArtUri ?: currentSong
                                    val imageRequest = remember(albumModel, currentSong?.id) {
                                        albumModel?.let {
                                            ImageRequest.Builder(context)
                                                .data(it)
                                                .crossfade(150)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .build()
                                        }
                                    }

                                    if (imageRequest != null) {
                                        AsyncImage(
                                            model = imageRequest,
                                            contentDescription = "Album Cover",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(if (isPlaying) rotation.value % 360f else 0f),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_rock_hand),
                                            contentDescription = "Rock Art",
                                            tint = skin.albumFrameColor,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Track Title, Artist, Hi-Res Badge
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(skin.albumFrameColor)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "HI-RES",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentSong?.format ?: "MP3",
                                        color = skin.textCyanColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = currentSong?.title ?: "No Rock Track Loaded",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = currentSong?.artist ?: "Rock Player",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Reactive Visualizer Bars
                        LockScreenVisualizerCanvas(
                            bars = visualizerBars,
                            activeColor = skin.albumFrameColor,
                            accentColor = skin.textCyanColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF101014))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Slider
                        var isDragging by remember { mutableStateOf(false) }
                        var dragPosMs by remember { mutableStateOf(0f) }

                        val effectivePos = if (isDragging) dragPosMs.toLong() else currentPosMs
                        val maxDuration = durationMs.coerceAtLeast(1L)

                        Slider(
                            value = (effectivePos.toFloat() / maxDuration).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                isDragging = true
                                dragPosMs = frac * maxDuration
                            },
                            onValueChangeFinished = {
                                engine.seekTo(dragPosMs.toLong())
                                isDragging = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = skin.albumFrameColor,
                                activeTrackColor = skin.albumFrameColor,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .testTag("lock_screen_slider")
                        )

                        // Time labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(effectivePos),
                                color = skin.textCyanColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatDuration(maxDuration),
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Transport Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle
                            IconButton(
                                onClick = { engine.setShuffle(!isShuffle) },
                                modifier = Modifier.size(38.dp).testTag("btn_lockscreen_shuffle")
                            ) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffle) skin.albumFrameColor else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Previous
                            IconButton(
                                onClick = { engine.playPrevious() },
                                modifier = Modifier.size(44.dp).testTag("btn_lockscreen_prev")
                            ) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Play / Pause (Vibrant Yellow Rock Button)
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(skin.playButtonColor)
                                    .border(2.dp, Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .clickable { engine.togglePlayPause() }
                                    .testTag("btn_lockscreen_play_pause"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color(0xFF101014),
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            // Next
                            IconButton(
                                onClick = { engine.playNext() },
                                modifier = Modifier.size(44.dp).testTag("btn_lockscreen_next")
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Repeat
                            IconButton(
                                onClick = { engine.cycleRepeatMode() },
                                modifier = Modifier.size(38.dp).testTag("btn_lockscreen_repeat")
                            ) {
                                Icon(
                                    imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != RepeatMode.OFF) skin.albumFrameColor else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. UNLOCK & OPEN APP ACTION BUTTON
                OutlinedButton(
                    onClick = onUnlockAndOpenApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_lockscreen_open_app"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = skin.albumFrameColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, skin.albumFrameColor)
                ) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock Device & Open Full Player",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun LockScreenVisualizerCanvas(
    bars: FloatArray,
    activeColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalBars = 20
        val spacing = 3.dp.toPx()
        val totalSpacing = spacing * (totalBars - 1)
        val barWidth = (size.width - totalSpacing) / totalBars

        for (i in 0 until totalBars) {
            val barFraction = bars.getOrElse(i) { 0.1f }.coerceIn(0.08f, 1.0f)
            val barHeight = size.height * barFraction
            val left = i * (barWidth + spacing)
            val top = size.height - barHeight

            val color = if (i < 6) accentColor else activeColor
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
