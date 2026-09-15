package com.example.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.audio.EqualizerBandInfo
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.model.VisualizerMode
import com.example.ui.components.EqualizerView
import com.example.ui.components.LibraryView
import com.example.ui.components.SettingsView
import com.example.ui.components.SkinPlayerView
import com.example.ui.theme.PlayerSkinTheme

@Composable
fun RockPlayerApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentSkin by viewModel.currentSkin.collectAsState()

    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val currentPositionMs by viewModel.audioEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.audioEngine.durationMs.collectAsState()
    val repeatMode by viewModel.audioEngine.repeatMode.collectAsState()
    val isShuffle by viewModel.audioEngine.isShuffle.collectAsState()

    val equalizerBands by viewModel.audioEngine.equalizerBands.collectAsState()
    val currentPreset by viewModel.audioEngine.currentPreset.collectAsState()
    val visualizerBars by viewModel.audioEngine.visualizerBars.collectAsState()
    val waveformPoints by viewModel.audioEngine.waveformPoints.collectAsState()
    val visualizerMode by viewModel.visualizerMode.collectAsState()

    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val autoPlayOnStart by viewModel.autoPlayOnStart.collectAsState()

    if (isLandscape) {
        // Landscape Mode: Side NavigationRail so the player gets maximum vertical height
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(currentSkin.backgroundColor)
        ) {
            NavigationRail(
                containerColor = currentSkin.surfaceColor,
                contentColor = Color.White,
                header = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rock_hand),
                        contentDescription = "Rock Player",
                        tint = currentSkin.albumFrameColor,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp)
                            .size(24.dp)
                    )
                },
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationRailItem(
                    selected = currentScreen == AppScreen.PLAYER,
                    onClick = { viewModel.navigateTo(AppScreen.PLAYER) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_rock_hand),
                            contentDescription = "Player",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("Player", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = currentSkin.albumFrameColor,
                        selectedTextColor = currentSkin.albumFrameColor,
                        indicatorColor = currentSkin.albumFrameColor.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_player")
                )

                NavigationRailItem(
                    selected = currentScreen == AppScreen.EQUALIZER,
                    onClick = { viewModel.navigateTo(AppScreen.EQUALIZER) },
                    icon = {
                        Icon(
                            Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("EQ & Viz", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = currentSkin.albumFrameColor,
                        selectedTextColor = currentSkin.albumFrameColor,
                        indicatorColor = currentSkin.albumFrameColor.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_equalizer")
                )

                NavigationRailItem(
                    selected = currentScreen == AppScreen.LIBRARY,
                    onClick = { viewModel.navigateTo(AppScreen.LIBRARY) },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Library",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("Tracks", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = currentSkin.textCyanColor,
                        selectedTextColor = currentSkin.textCyanColor,
                        indicatorColor = currentSkin.textCyanColor.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_library")
                )

                NavigationRailItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("Settings", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = currentSkin.controlIconsColor,
                        selectedTextColor = currentSkin.controlIconsColor,
                        indicatorColor = currentSkin.controlIconsColor.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Mini Player (Visible in landscape when in Equalizer, Library or Settings)
                AnimatedVisibility(
                    visible = currentScreen != AppScreen.PLAYER && currentSong != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    MiniPlayerBar(
                        song = currentSong,
                        isPlaying = isPlaying,
                        skin = currentSkin,
                        onBarClick = { viewModel.navigateTo(AppScreen.PLAYER) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNext() }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AppScreenContent(
                        currentScreen = currentScreen,
                        viewModel = viewModel,
                        currentSkin = currentSkin,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        repeatMode = repeatMode,
                        isShuffle = isShuffle,
                        equalizerBands = equalizerBands,
                        currentPreset = currentPreset,
                        visualizerBars = visualizerBars,
                        waveformPoints = waveformPoints,
                        visualizerMode = visualizerMode,
                        filteredSongs = filteredSongs,
                        searchQuery = searchQuery,
                        isScanning = isScanning,
                        statusMessage = statusMessage,
                        autoPlayOnStart = autoPlayOnStart
                    )
                }
            }
        }
    } else {
        // Portrait Mode: Standard Scaffold with Bottom Navigation
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = currentSkin.backgroundColor,
            bottomBar = {
                Column {
                    // Mini Player (Visible when in Equalizer, Library or Settings)
                    AnimatedVisibility(
                        visible = currentScreen != AppScreen.PLAYER && currentSong != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        MiniPlayerBar(
                            song = currentSong,
                            isPlaying = isPlaying,
                            skin = currentSkin,
                            onBarClick = { viewModel.navigateTo(AppScreen.PLAYER) },
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNext() }
                        )
                    }

                    // Bottom Navigation
                    NavigationBar(
                        containerColor = currentSkin.surfaceColor,
                        contentColor = Color.White,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.PLAYER,
                            onClick = { viewModel.navigateTo(AppScreen.PLAYER) },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_rock_hand),
                                    contentDescription = "Player",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Player", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = currentSkin.albumFrameColor,
                                selectedTextColor = currentSkin.albumFrameColor,
                                indicatorColor = currentSkin.albumFrameColor.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_tab_player")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.EQUALIZER,
                            onClick = { viewModel.navigateTo(AppScreen.EQUALIZER) },
                            icon = {
                                Icon(
                                    Icons.Default.Equalizer,
                                    contentDescription = "Equalizer",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("EQ & Viz", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = currentSkin.albumFrameColor,
                                selectedTextColor = currentSkin.albumFrameColor,
                                indicatorColor = currentSkin.albumFrameColor.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_tab_equalizer")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.LIBRARY,
                            onClick = { viewModel.navigateTo(AppScreen.LIBRARY) },
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "Library",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Tracks", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = currentSkin.textCyanColor,
                                selectedTextColor = currentSkin.textCyanColor,
                                indicatorColor = currentSkin.textCyanColor.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_tab_library")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SETTINGS,
                            onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                            icon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = currentSkin.controlIconsColor,
                                selectedTextColor = currentSkin.controlIconsColor,
                                indicatorColor = currentSkin.controlIconsColor.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AppScreenContent(
                    currentScreen = currentScreen,
                    viewModel = viewModel,
                    currentSkin = currentSkin,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    equalizerBands = equalizerBands,
                    currentPreset = currentPreset,
                    visualizerBars = visualizerBars,
                    waveformPoints = waveformPoints,
                    visualizerMode = visualizerMode,
                    filteredSongs = filteredSongs,
                    searchQuery = searchQuery,
                    isScanning = isScanning,
                    statusMessage = statusMessage,
                    autoPlayOnStart = autoPlayOnStart
                )
            }
        }
    }
}

@Composable
private fun AppScreenContent(
    currentScreen: AppScreen,
    viewModel: MainViewModel,
    currentSkin: PlayerSkinTheme,
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    equalizerBands: List<EqualizerBandInfo>,
    currentPreset: String,
    visualizerBars: FloatArray,
    waveformPoints: FloatArray,
    visualizerMode: VisualizerMode,
    filteredSongs: List<Song>,
    searchQuery: String,
    isScanning: Boolean,
    statusMessage: String?,
    autoPlayOnStart: Boolean
) {
    when (currentScreen) {
        AppScreen.PLAYER -> {
            SkinPlayerView(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                skin = currentSkin,
                visualizerBars = visualizerBars,
                waveform = waveformPoints,
                visualizerMode = visualizerMode,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onOpenEqualizer = { viewModel.navigateTo(AppScreen.EQUALIZER) },
                onOpenLibrary = { viewModel.navigateTo(AppScreen.LIBRARY) },
                onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                onVisualizerModeChange = { viewModel.setVisualizerMode(it) }
            )
        }

        AppScreen.EQUALIZER -> {
            EqualizerView(
                bands = equalizerBands,
                currentPreset = currentPreset,
                visualizerBars = visualizerBars,
                waveform = waveformPoints,
                visualizerMode = visualizerMode,
                isPlaying = isPlaying,
                skin = currentSkin,
                onBandChange = { bandIndex, db -> viewModel.setEqualizerBand(bandIndex, db) },
                onPresetSelect = { viewModel.applyEqualizerPreset(it) },
                onVisualizerModeChange = { viewModel.setVisualizerMode(it) }
            )
        }

        AppScreen.LIBRARY -> {
            LibraryView(
                songs = filteredSongs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                isScanning = isScanning,
                statusMessage = statusMessage,
                searchQuery = searchQuery,
                skin = currentSkin,
                onSongClick = { viewModel.playSong(it) },
                onFolderPicked = { viewModel.scanFolderUri(it) },
                onScanDevice = { viewModel.scanDeviceMediaStore() },
                onSearchChange = { viewModel.onSearchQueryChange(it) },
                onClearStatus = { viewModel.clearStatusMessage() }
            )
        }

        AppScreen.SETTINGS -> {
            SettingsView(
                autoPlayOnStart = autoPlayOnStart,
                currentSkin = currentSkin,
                onAutoPlayChange = { viewModel.setAutoPlayOnStart(it) },
                onSelectSkin = { viewModel.setSkinTheme(it) }
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(
    song: Song?,
    isPlaying: Boolean,
    skin: PlayerSkinTheme,
    onBarClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    if (song == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(skin.surfaceColor)
            .border(1.dp, skin.albumFrameColor.copy(alpha = 0.4f))
            .clickable { onBarClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("mini_player_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yellow square album art thumbnail as in sfondo.jpg!
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(skin.albumFrameColor)
                    .padding(2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Mini Album Art",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rock_hand),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = song.title,
                    color = skin.textCyanColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.format}",
                    color = skin.textSecondaryColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Play/Pause red button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(skin.playButtonColor)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = skin.controlIconsColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
