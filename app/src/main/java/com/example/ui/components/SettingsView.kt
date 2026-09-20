package com.example.ui.components

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.example.ui.widget.RockPlayerWidgetProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.image.AlbumArtDiskCache
import com.example.data.repository.MusicScanner
import com.example.ui.theme.PlayerSkinTheme
import com.example.ui.theme.PlayerSkins

@Composable
fun SettingsView(
    autoPlayOnStart: Boolean,
    currentSkin: PlayerSkinTheme,
    rememberedFolderName: String? = null,
    isFolderAuthorized: Boolean = false,
    lockScreenPlayerEnabled: Boolean = true,
    wakeScreenOnTrackChange: Boolean = false,
    onAutoPlayChange: (Boolean) -> Unit,
    onSelectSkin: (PlayerSkinTheme) -> Unit,
    onLockScreenPlayerChange: (Boolean) -> Unit = {},
    onWakeScreenOnTrackChange: (Boolean) -> Unit = {},
    onPreviewLockScreenPlayer: () -> Unit = {},
    onFolderPicked: (Uri) -> Unit = {},
    onRescanFolder: () -> Unit = {},
    onClearFolder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            MusicScanner.takeAndPersistFolderPermission(context, uri)
            onFolderPicked(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(currentSkin.backgroundColor),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(currentSkin.albumFrameColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = currentSkin.albumFrameColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ROCK SETTINGS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Player options & customizable dark themes",
                    color = currentSkin.textSecondaryColor,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 1: PLAYBACK OPTIONS (Auto Play on Start)
        Text(
            text = "PLAYBACK AUTOMATION",
            color = currentSkin.textCyanColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = currentSkin.surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.cardBorderColor),
            modifier = Modifier.fillMaxWidth().testTag("auto_play_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentSkin.playButtonColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayCircleFilled,
                            contentDescription = null,
                            tint = currentSkin.playButtonColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Auto-play on App Start",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Automatically start playing songs as soon as Rock Player opens",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = autoPlayOnStart,
                    onCheckedChange = onAutoPlayChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = currentSkin.playButtonColor,
                        checkedTrackColor = currentSkin.albumFrameColor,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF2A2A32)
                    ),
                    modifier = Modifier.testTag("auto_play_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: REMEMBERED MUSIC FOLDER
        Text(
            text = "MUSIC FOLDER MEMORY",
            color = currentSkin.textCyanColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = currentSkin.surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.cardBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_remembered_folder_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentSkin.albumFrameColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = currentSkin.albumFrameColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Selected Folder",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (rememberedFolderName != null) rememberedFolderName else "No folder remembered yet",
                                color = if (rememberedFolderName != null) currentSkin.albumFrameColor else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (rememberedFolderName != null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (rememberedFolderName != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isFolderAuthorized) Color(0xFF00E676) else currentSkin.albumFrameColor)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isFolderAuthorized) "✓ AUTHORIZED" else "REMEMBERED",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(currentSkin.textCyanColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FAST CACHE",
                                    color = currentSkin.textCyanColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isFolderAuthorized)
                        "Folder permission is permanently remembered. Tracks are indexed into local cache for instant launch with zero waiting time."
                    else
                        "The app remembers this folder and caches all your audio tracks for instant playback on start.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentSkin.albumFrameColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (rememberedFolderName != null) "Change Folder" else "Select Folder",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (rememberedFolderName != null) {
                        OutlinedButton(
                            onClick = onRescanFolder,
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(currentSkin.textCyanColor.copy(alpha = 0.7f))
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = currentSkin.textCyanColor)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onClearFolder,
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.5f))
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Forget", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: HOME SCREEN MINI PLAYER WIDGET
        Text(
            text = "HOME SCREEN MINI PLAYER",
            color = currentSkin.textCyanColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = currentSkin.surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.cardBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_home_widget_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = currentSkin.albumFrameColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Android Home Screen Widget",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Control rock playback, view track titles, and skip songs directly from your phone's home screen without reopening the app.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Mini Player Widget Preview Mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1E28))
                        .border(1.dp, Color(0xFF363C4E), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Frame
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF12131A))
                                .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Electric Thunder",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF2A2F3D))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "HI-RES",
                                        color = Color(0xFFFFD700),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Iron Riff • Rock Mini Player",
                                color = Color(0xFFC0C4CC),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                            val provider = ComponentName(context, RockPlayerWidgetProvider::class.java)
                            appWidgetManager.requestPinAppWidget(provider, null, null)
                        } else {
                            Toast.makeText(
                                context,
                                "Touch & hold your home screen, choose 'Widgets', and select Rock Player Mini!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentSkin.albumFrameColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_pin_home_widget")
                ) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Mini Player to Home Screen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: CUSTOMIZABLE DARK THEME
        Text(
            text = "CUSTOMIZABLE DARK THEMES",
            color = currentSkin.textCyanColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(currentSkin.surfaceColor)
                .border(1.dp, currentSkin.cardBorderColor, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerSkins.allSkins.forEach { skin ->
                val isSelected = currentSkin.id == skin.id
                SkinThemeOptionItem(
                    skin = skin,
                    isSelected = isSelected,
                    onSelect = { onSelectSkin(skin) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: HI-RES AUDIO FORMAT SUPPORT
        Text(
            text = "AUDIO ENGINE & HI-RES CAPABILITY",
            color = currentSkin.textCyanColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = currentSkin.surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = currentSkin.albumFrameColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lossless & Hi-Res Format Decoder",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val formats = listOf(
                    "FLAC" to "24-bit / 96kHz Lossless",
                    "WAV" to "Uncompressed Linear PCM",
                    "MP3" to "MPEG-1 Layer 3 (up to 320kbps)",
                    "AAC / M4A" to "Advanced Audio Coding",
                    "OGG / OPUS" to "Low-latency Vorbis Stream"
                )

                formats.forEach { (fmt, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fmt,
                            color = currentSkin.textCyanColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = desc,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 4: LOCAL DISK CACHE & HARDWARE ACCELERATION
        Text(
            text = "ALBUM ARTWORK CACHE & PERFORMANCE",
            color = currentSkin.textCyanColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        var cacheSizeBytes by remember {
            mutableStateOf(AlbumArtDiskCache.getCacheSizeBytes(context))
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = currentSkin.surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.cardBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_album_art_cache")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = currentSkin.albumFrameColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Coil Local Disk Cache",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "High-speed local disk caching with GPU hardware-bitmap decoding. Eliminates repeated ContentResolver and Storage Access Framework disk reads during list scrolling, saving battery and ensuring 60/120 FPS performance.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                val cacheMb = "%.2f MB".format(cacheSizeBytes / (1024.0 * 1024.0))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF16161D))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Disk Cache Size",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = cacheMb,
                        color = currentSkin.albumFrameColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        AlbumArtDiskCache.clearCache(context)
                        cacheSizeBytes = AlbumArtDiskCache.getCacheSizeBytes(context)
                        Toast.makeText(context, "Album artwork disk cache cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_clear_art_cache"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = currentSkin.albumFrameColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.albumFrameColor.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear Album Artwork Cache",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 5: LOCK SCREEN MINI PLAYER
        Text(
            text = "LOCK SCREEN MINI PLAYER",
            color = currentSkin.textCyanColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = currentSkin.surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.cardBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_lockscreen_settings")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Show Mini Player When Device Is Locked",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Display playback controls, album cover, and real-time visualizer directly on the lock screen without unlocking your phone.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = lockScreenPlayerEnabled,
                        onCheckedChange = onLockScreenPlayerChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentSkin.albumFrameColor,
                            checkedTrackColor = currentSkin.albumFrameColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("switch_lockscreen_player")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Wake Screen On Track Change",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Briefly illuminate the lock screen to preview album art and title when a new rock song begins.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = wakeScreenOnTrackChange,
                        onCheckedChange = onWakeScreenOnTrackChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentSkin.albumFrameColor,
                            checkedTrackColor = currentSkin.albumFrameColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("switch_wake_screen")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onPreviewLockScreenPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_preview_lockscreen"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = currentSkin.albumFrameColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.albumFrameColor)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preview Lock Screen Mini Player",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Branding Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_rock_hand),
                contentDescription = null,
                tint = currentSkin.albumFrameColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Rock Player • Engineered for True Rock & Metal",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
}

@Composable
private fun SkinThemeOptionItem(
    skin: PlayerSkinTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0x33FFFFFF) else Color(0x10FFFFFF))
            .border(
                1.dp,
                if (isSelected) skin.albumFrameColor else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("theme_option_${skin.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = skin.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Color palette swatches
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(skin.albumFrameColor)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(skin.textCyanColor)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(skin.playButtonColor)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(skin.backgroundColor)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(skin.albumFrameColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
