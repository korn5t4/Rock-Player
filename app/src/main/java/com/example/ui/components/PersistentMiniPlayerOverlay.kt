package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.example.R
import com.example.data.model.Song
import com.example.ui.theme.PlayerSkinTheme

/**
 * Persistent Mini-Player Overlay that remains visible across screens (Library, Equalizer, Settings)
 * for convenient, instant media control without leaving the current view.
 */
@Composable
fun PersistentMiniPlayerOverlay(
    audioEngine: com.example.data.audio.RockAudioEngine,
    skin: PlayerSkinTheme,
    onBarClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by audioEngine.currentSong.collectAsState()
    if (song == null) return

    val isPlaying by audioEngine.isPlaying.collectAsState()
    val currentPositionMs by audioEngine.currentPositionMs.collectAsState()
    val durationMs by audioEngine.durationMs.collectAsState()
    val visualizerBars by audioEngine.visualizerBars.collectAsState()

    PersistentMiniPlayerOverlay(
        song = song,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        visualizerBars = visualizerBars,
        skin = skin,
        onBarClick = onBarClick,
        onPlayPause = onPlayPause,
        onNext = onNext,
        onPrevious = onPrevious,
        modifier = modifier
    )
}

@Composable
fun PersistentMiniPlayerOverlay(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    visualizerBars: FloatArray,
    skin: PlayerSkinTheme,
    onBarClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        skin.surfaceColor.copy(alpha = 0.96f),
                        Color(0xFF0D0D12)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        skin.albumFrameColor.copy(alpha = 0.7f),
                        skin.textCyanColor.copy(alpha = 0.5f),
                        skin.playButtonColor.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onBarClick
            )
            .testTag("mini_player_overlay")
    ) {
        // TOP TRACK PROGRESS BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFF22222B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                skin.visualizerColor,
                                skin.albumFrameColor,
                                skin.playButtonColor
                            )
                        )
                    )
            )
        }

        // MAIN CONTENT ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT: ALBUM ART THUMBNAIL WITH RETRO BORDER
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(skin.albumFrameColor)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val miniImageRequest = remember(song.id, song.albumArtUriString) {
                        ImageRequest.Builder(context)
                            .data(song)
                            .size(coil.size.Size(128, 128))
                            .scale(Scale.FILL)
                            .precision(Precision.INEXACT)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .crossfade(150)
                            .build()
                    }
                    AsyncImage(
                        model = miniImageRequest,
                        contentDescription = "Cover for ${song.title}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                        fallback = painterResource(id = R.drawable.ic_rock_hand),
                        error = painterResource(id = R.drawable.ic_rock_hand)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // CENTER: TRACK METADATA & TIMESTAMP
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("mini_song_title")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = song.artist,
                            color = skin.textCyanColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Text(
                            text = " • ",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        Text(
                            text = song.format,
                            color = if (song.isHiRes) Color(0xFFFFD700) else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // TIME ELAPSED / DURATION
                    Text(
                        text = "${formatDuration(currentPositionMs)} / ${formatDuration(durationMs.coerceAtLeast(1000L))}",
                        color = skin.textSecondaryColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // MINI VISUALIZER BARS (Active spectrum feedback)
                if (visualizerBars.isNotEmpty()) {
                    MiniSpectrumCanvas(
                        bars = visualizerBars,
                        isPlaying = isPlaying,
                        skin = skin,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(width = 24.dp, height = 22.dp)
                    )
                }
            }

            // RIGHT: PLAYBACK CONTROLS (Previous, Play/Pause, Next, Expand)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Previous Track Button
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(38.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("mini_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = skin.controlIconsColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play / Pause Button with glowing red gradient
                val playButtonScale by animateFloatAsState(
                    targetValue = if (isPlaying) 1.05f else 1.0f,
                    animationSpec = tween(120),
                    label = "mini_play_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(playButtonScale)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    skin.playButtonColor,
                                    Color(0xFF990000)
                                )
                            )
                        )
                        .border(1.5.dp, Color(0x77FFFFFF), CircleShape)
                        .clickable(onClick = onPlayPause)
                        .minimumInteractiveComponentSize()
                        .testTag("mini_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause track" else "Play track",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Next Track Button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(38.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next track",
                        tint = skin.controlIconsColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Expand Chevron Button
                IconButton(
                    onClick = onBarClick,
                    modifier = Modifier
                        .size(32.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("mini_expand_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Open full player",
                        tint = skin.albumFrameColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniSpectrumCanvas(
    bars: FloatArray,
    isPlaying: Boolean,
    skin: PlayerSkinTheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val numBars = 5
        val spacing = 2.dp.toPx()
        val totalSpacing = spacing * (numBars - 1)
        val barWidth = ((size.width - totalSpacing) / numBars).coerceAtLeast(1.5f)

        for (i in 0 until numBars) {
            val mag = if (isPlaying && bars.isNotEmpty()) {
                bars.getOrElse(i * 3) { 0.1f }.coerceIn(0.12f, 1f)
            } else {
                0.08f
            }
            val barHeight = size.height * mag
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight

            drawRoundRect(
                color = if (i % 2 == 0) skin.albumFrameColor else skin.visualizerColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
