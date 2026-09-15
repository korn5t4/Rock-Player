package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.R
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.model.VisualizerMode
import com.example.data.repository.AlbumArtResolver
import com.example.ui.theme.PlayerSkinTheme

@Composable
fun SkinPlayerView(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    skin: PlayerSkinTheme,
    visualizerBars: FloatArray,
    waveform: FloatArray,
    visualizerMode: VisualizerMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onVisualizerModeChange: (VisualizerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(skin.backgroundColor)
    ) {
        val isRotatedLandscape = isLandscape || (maxWidth > maxHeight && maxWidth > 480.dp)

        if (isRotatedLandscape) {
            // When device is rotated: album art is in the left and in the right the other functions
            LandscapePlayerLayout(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                skin = skin,
                visualizerBars = visualizerBars,
                waveform = waveform,
                visualizerMode = visualizerMode,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onOpenEqualizer = onOpenEqualizer,
                onOpenLibrary = onOpenLibrary,
                onOpenSettings = onOpenSettings,
                onVisualizerModeChange = onVisualizerModeChange
            )
        } else {
            // Portrait layout
            PortraitPlayerLayout(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                skin = skin,
                visualizerBars = visualizerBars,
                waveform = waveform,
                visualizerMode = visualizerMode,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onOpenEqualizer = onOpenEqualizer,
                onOpenLibrary = onOpenLibrary,
                onOpenSettings = onOpenSettings,
                onVisualizerModeChange = onVisualizerModeChange
            )
        }
    }
}

@Composable
private fun LandscapePlayerLayout(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    skin: PlayerSkinTheme,
    visualizerBars: FloatArray,
    waveform: FloatArray,
    visualizerMode: VisualizerMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onVisualizerModeChange: (VisualizerMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(skin.surfaceColor)
            .border(2.dp, skin.cardBorderColor, RoundedCornerShape(20.dp))
            .padding(12.dp)
            .testTag("sfondo_player_skin")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // THE ALBUM ART IS IN THE LEFT
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtSquare(
                    currentSong = currentSong,
                    skin = skin,
                    modifier = Modifier
                        .fillMaxHeight(0.96f)
                        .aspectRatio(1.0f)
                )
            }

            // AND IN THE RIGHT THE OTHER FUNCTIONS
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Bar with branding & Navigation
                PlayerTopBarSection(
                    skin = skin,
                    onOpenEqualizer = onOpenEqualizer,
                    onOpenLibrary = onOpenLibrary,
                    onOpenSettings = onOpenSettings
                )

                // Song Metadata in Cyan
                SongMetadataSection(
                    currentSong = currentSong,
                    skin = skin
                )

                // Seekbar & Timestamps
                PlaybackSeekbarSection(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    fallbackDurationMs = currentSong?.durationMs ?: 0L,
                    skin = skin,
                    onSeek = onSeek
                )

                // Playback Controls with Red Triangle Play Button
                PlaybackControlsSection(
                    isPlaying = isPlaying,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    skin = skin,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    isCompact = true
                )

                // Real-Time Visualizer
                RealTimeVisualizerView(
                    bars = visualizerBars,
                    waveform = waveform,
                    mode = visualizerMode,
                    skin = skin,
                    isPlaying = isPlaying,
                    onModeChange = onVisualizerModeChange
                )
            }
        }
    }
}

@Composable
private fun PortraitPlayerLayout(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    skin: PlayerSkinTheme,
    visualizerBars: FloatArray,
    waveform: FloatArray,
    visualizerMode: VisualizerMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onVisualizerModeChange: (VisualizerMode) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar / Branding
        PlayerTopBarSection(
            skin = skin,
            onOpenEqualizer = onOpenEqualizer,
            onOpenLibrary = onOpenLibrary,
            onOpenSettings = onOpenSettings
        )

        // SFONDO SKIN CONTAINER: Faithful to the uploaded layout in sfondo.jpg!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(skin.surfaceColor)
                .border(2.dp, skin.cardBorderColor, RoundedCornerShape(20.dp))
                .padding(18.dp)
                .testTag("sfondo_player_skin")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. YELLOW SQUARE FOR ALBUM ART (Exact design match for sfondo.jpg)
                AlbumArtSquare(
                    currentSong = currentSong,
                    skin = skin,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1.0f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. SONG METADATA IN CYAN (Exact match for sfondo.jpg!)
                SongMetadataSection(
                    currentSong = currentSong,
                    skin = skin
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Seekbar & Timing
                PlaybackSeekbarSection(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    fallbackDurationMs = currentSong?.durationMs ?: 0L,
                    skin = skin,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. PLAYBACK CONTROLS WITH THE RED TRIANGLE PLAY BUTTON (As in sfondo.jpg!)
                PlaybackControlsSection(
                    isPlaying = isPlaying,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    skin = skin,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    isCompact = false
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Real-Time Visualizer integrated below the main skin
        RealTimeVisualizerView(
            bars = visualizerBars,
            waveform = waveform,
            mode = visualizerMode,
            skin = skin,
            isPlaying = isPlaying,
            onModeChange = onVisualizerModeChange
        )
    }
}

@Composable
private fun AlbumArtSquare(
    currentSong: Song?,
    skin: PlayerSkinTheme,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var resolvedArtUri by remember(currentSong?.id, currentSong?.albumArtUriString) {
        mutableStateOf(currentSong?.albumArtUri)
    }

    LaunchedEffect(currentSong?.id, currentSong?.uriString) {
        if (currentSong != null) {
            val resolved = AlbumArtResolver.resolveAlbumArt(context, currentSong)
            if (resolved != null) {
                resolvedArtUri = resolved
            }
        }
    }

    // Iconic yellow square matching sfondo.jpg design
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(skin.albumFrameColor) // Vibrant yellow square!
            .border(3.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp))
            .padding(6.dp) // Yellow matting frame around the *.jpg album cover
            .testTag("album_art_square"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF101014)),
            contentAlignment = Alignment.Center
        ) {
            if (resolvedArtUri != null) {
                // High-fidelity *.jpg album cover of the file played
                AsyncImage(
                    model = resolvedArtUri,
                    contentDescription = currentSong?.let { "${it.title} - Album Cover" } ?: "Album Cover",
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("album_art_image"),
                    contentScale = ContentScale.Crop
                )

                // Fine inner border for vintage physical album sleeve finish
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                )
            } else {
                // High-energy Rock Art Graphic fallback inside yellow square
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rock_hand),
                        contentDescription = "Rock Art",
                        tint = Color(0xFF101014),
                        modifier = Modifier
                            .size(100.dp)
                            .testTag("rock_hand_art")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ROCK ON",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Format Badge in upper corner
            if (currentSong != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, skin.albumFrameColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentSong.formatBadgeText,
                        color = if (currentSong.isHiRes) Color(0xFFFFD700) else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SongMetadataSection(
    currentSong: Song?,
    skin: PlayerSkinTheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = currentSong?.title ?: "Select a Rock Song",
            color = skin.textCyanColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("song_title_text")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = currentSong?.artist ?: "Rock Player",
            color = skin.textCyanColor.copy(alpha = 0.9f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("song_artist_text")
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = currentSong?.album ?: "Rock Album Vault",
            color = skin.textCyanColor.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("song_album_text")
        )
    }
}

@Composable
private fun PlaybackSeekbarSection(
    currentPositionMs: Long,
    durationMs: Long,
    fallbackDurationMs: Long,
    skin: PlayerSkinTheme,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val progress = if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Slider(
            value = progress,
            onValueChange = { frac ->
                onSeek((frac * durationMs).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = skin.playButtonColor,
                activeTrackColor = skin.albumFrameColor,
                inactiveTrackColor = Color(0xFF2E2E36)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("playback_seek_slider")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMillis(currentPositionMs),
                color = skin.textSecondaryColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = formatMillis(durationMs.coerceAtLeast(fallbackDurationMs)),
                color = skin.textSecondaryColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PlaybackControlsSection(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    skin: PlayerSkinTheme,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.testTag("shuffle_button")
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) skin.albumFrameColor else Color.Gray,
                modifier = Modifier.size(if (isCompact) 22.dp else 24.dp)
            )
        }

        // Previous Track Button
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.testTag("previous_button")
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous Song",
                tint = skin.controlIconsColor,
                modifier = Modifier.size(if (isCompact) 32.dp else 36.dp)
            )
        }

        // RED PLAY TRIANGLE / PAUSE BUTTON (Exact focal point of sfondo.jpg)
        val playButtonScale by animateFloatAsState(
            targetValue = if (isPlaying) 1.05f else 1.0f,
            animationSpec = tween(150),
            label = "play_btn_scale"
        )

        val btnSize = if (isCompact) 62.dp else 72.dp
        val iconSize = if (isCompact) 38.dp else 44.dp

        Box(
            modifier = Modifier
                .scale(playButtonScale)
                .size(btnSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            skin.playButtonColor,
                            Color(0xFFB30000)
                        )
                    )
                )
                .border(2.dp, Color(0x66FFFFFF), CircleShape)
                .shadow(12.dp, CircleShape)
                .clickable { onPlayPause() }
                .testTag("play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }

        // Next Track Button
        IconButton(
            onClick = onNext,
            modifier = Modifier.testTag("next_button")
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next Song",
                tint = skin.controlIconsColor,
                modifier = Modifier.size(if (isCompact) 32.dp else 36.dp)
            )
        }

        // Repeat Button
        IconButton(
            onClick = onCycleRepeat,
            modifier = Modifier.testTag("repeat_button")
        ) {
            val icon = when (repeatMode) {
                RepeatMode.ONE -> Icons.Default.RepeatOne
                else -> Icons.Default.Repeat
            }
            val tint = when (repeatMode) {
                RepeatMode.OFF -> Color.Gray
                RepeatMode.ALL -> skin.albumFrameColor
                RepeatMode.ONE -> skin.playButtonColor
            }
            Icon(
                icon,
                contentDescription = "Repeat: $repeatMode",
                tint = tint,
                modifier = Modifier.size(if (isCompact) 22.dp else 24.dp)
            )
        }
    }
}

@Composable
private fun PlayerTopBarSection(
    skin: PlayerSkinTheme,
    onOpenEqualizer: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_rock_hand),
                contentDescription = "Rock Player",
                tint = skin.albumFrameColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ROCK PLAYER",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onOpenEqualizer,
                modifier = Modifier.testTag("nav_equalizer_button")
            ) {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = "Equalizer",
                    tint = skin.albumFrameColor
                )
            }

            IconButton(
                onClick = onOpenLibrary,
                modifier = Modifier.testTag("nav_library_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Library",
                    tint = skin.textCyanColor
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("nav_settings_button")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = skin.controlIconsColor
                )
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

