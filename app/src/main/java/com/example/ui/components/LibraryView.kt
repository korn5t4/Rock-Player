package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.example.R
import com.example.data.model.Song
import com.example.data.repository.MusicScanner
import com.example.data.usb.UsbFileItem
import com.example.data.usb.UsbStorageDevice
import com.example.ui.LibrarySourceFilter
import com.example.ui.theme.PlayerSkinTheme

@Composable
fun LibraryView(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isScanning: Boolean,
    statusMessage: String?,
    searchQuery: String,
    skin: PlayerSkinTheme,
    rememberedFolderName: String? = null,
    rememberedFolderUri: String? = null,
    isFolderAuthorized: Boolean = false,
    connectedUsbDevices: List<UsbStorageDevice> = emptyList(),
    usbStatusMessage: String? = null,
    sourceFilter: LibrarySourceFilter = LibrarySourceFilter.ALL,
    onSourceFilterChange: (LibrarySourceFilter) -> Unit = {},
    onRefreshUsbDevices: () -> Unit = {},
    onExploreUsbDirectory: suspend (UsbStorageDevice, String?, Uri?) -> List<UsbFileItem> = { _, _, _ -> emptyList() },
    onPlayUsbFileDirect: (UsbFileItem, UsbStorageDevice) -> Unit = { _, _ -> },
    onScanUsbDevice: (UsbStorageDevice, Uri?) -> Unit = { _, _ -> },
    onSongClick: (Song) -> Unit,
    onFolderPicked: (Uri) -> Unit,
    onRescanFolder: () -> Unit = {},
    onClearFolder: () -> Unit = {},
    onScanDevice: () -> Unit,
    onSearchChange: (String) -> Unit,
    onClearStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeUsbDeviceForExplorer by remember { mutableStateOf<UsbStorageDevice?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            MusicScanner.takeAndPersistFolderPermission(context, uri)
            onFolderPicked(uri)
        }
    }

    // Active USB Explorer Modal Dialog
    activeUsbDeviceForExplorer?.let { dev ->
        UsbExplorerDialog(
            device = dev,
            skin = skin,
            onDismiss = { activeUsbDeviceForExplorer = null },
            onExploreDirectory = onExploreUsbDirectory,
            onPlayFileDirect = onPlayUsbFileDirect,
            onScanFolder = onScanUsbDevice,
            onRequestSafPermission = {
                folderPickerLauncher.launch(null)
            }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(skin.backgroundColor)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LibraryContent(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isScanning = isScanning,
                    statusMessage = statusMessage,
                    searchQuery = searchQuery,
                    skin = skin,
                    rememberedFolderName = rememberedFolderName,
                    rememberedFolderUri = rememberedFolderUri,
                    isFolderAuthorized = isFolderAuthorized,
                    connectedUsbDevices = connectedUsbDevices,
                    usbStatusMessage = usbStatusMessage,
                    sourceFilter = sourceFilter,
                    onSourceFilterChange = onSourceFilterChange,
                    onRefreshUsbDevices = onRefreshUsbDevices,
                    onOpenUsbExplorer = { activeUsbDeviceForExplorer = it },
                    onScanUsbDevice = onScanUsbDevice,
                    onSongClick = onSongClick,
                    onFolderPicked = onFolderPicked,
                    onRescanFolder = onRescanFolder,
                    onClearFolder = onClearFolder,
                    onScanDevice = onScanDevice,
                    onSearchChange = onSearchChange,
                    onClearStatus = onClearStatus,
                    folderPickerLauncher = folderPickerLauncher,
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(skin.albumFrameColor.copy(alpha = 0.25f))
                )

                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LibraryDetailPane(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        skin = skin,
                        onSongClick = onSongClick
                    )
                }
            }
        } else {
            LibraryContent(
                songs = songs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                isScanning = isScanning,
                statusMessage = statusMessage,
                searchQuery = searchQuery,
                skin = skin,
                rememberedFolderName = rememberedFolderName,
                rememberedFolderUri = rememberedFolderUri,
                isFolderAuthorized = isFolderAuthorized,
                connectedUsbDevices = connectedUsbDevices,
                usbStatusMessage = usbStatusMessage,
                sourceFilter = sourceFilter,
                onSourceFilterChange = onSourceFilterChange,
                onRefreshUsbDevices = onRefreshUsbDevices,
                onOpenUsbExplorer = { activeUsbDeviceForExplorer = it },
                onScanUsbDevice = onScanUsbDevice,
                onSongClick = onSongClick,
                onFolderPicked = onFolderPicked,
                onRescanFolder = onRescanFolder,
                onClearFolder = onClearFolder,
                onScanDevice = onScanDevice,
                onSearchChange = onSearchChange,
                onClearStatus = onClearStatus,
                folderPickerLauncher = folderPickerLauncher,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun LibraryContent(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isScanning: Boolean,
    statusMessage: String?,
    searchQuery: String,
    skin: PlayerSkinTheme,
    rememberedFolderName: String?,
    rememberedFolderUri: String?,
    isFolderAuthorized: Boolean,
    connectedUsbDevices: List<UsbStorageDevice>,
    usbStatusMessage: String?,
    sourceFilter: LibrarySourceFilter,
    onSourceFilterChange: (LibrarySourceFilter) -> Unit,
    onRefreshUsbDevices: () -> Unit,
    onOpenUsbExplorer: (UsbStorageDevice) -> Unit,
    onScanUsbDevice: (UsbStorageDevice, Uri?) -> Unit,
    onSongClick: (Song) -> Unit,
    onFolderPicked: (Uri) -> Unit,
    onRescanFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onScanDevice: () -> Unit,
    onSearchChange: (String) -> Unit,
    onClearStatus: () -> Unit,
    folderPickerLauncher: androidx.activity.result.ActivityResultLauncher<Uri?>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ROCK LIBRARY",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${songs.size} tracks available (Hi-Res & MP3)",
                    color = skin.textSecondaryColor,
                    fontSize = 12.sp
                )
            }

            if (isScanning) {
                CircularProgressIndicator(
                    color = skin.albumFrameColor,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // REMEMBERED FOLDER CARD (Prominently shows the remembered folder & controls)
        if (rememberedFolderName != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = skin.surfaceColor),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, skin.albumFrameColor.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("remembered_folder_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                .background(skin.albumFrameColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = skin.albumFrameColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isFolderAuthorized) Color(0xFF00E676) else skin.albumFrameColor)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isFolderAuthorized) "✓ AUTHORIZED (ACCESS RETAINED)" else "REMEMBERED FOLDER",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(skin.textCyanColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "⚡ FAST SCAN",
                                        color = skin.textCyanColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = rememberedFolderName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Permission saved • Loaded instantly from local cache",
                                color = skin.textCyanColor.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Rescan Button
                        IconButton(
                            onClick = onRescanFolder,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("rescan_folder_button")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Rescan folder",
                                tint = skin.albumFrameColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // Clear / Forget Button
                        IconButton(
                            onClick = onClearFolder,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("clear_folder_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Forget remembered folder",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // FOLDER SCAN ACTION BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Button: Add/Change Music Folder
            Button(
                onClick = { folderPickerLauncher.launch(null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (rememberedFolderName != null) skin.surfaceColor else skin.albumFrameColor,
                    contentColor = if (rememberedFolderName != null) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                border = if (rememberedFolderName != null) {
                    androidx.compose.foundation.BorderStroke(1.dp, skin.albumFrameColor.copy(alpha = 0.6f))
                } else null,
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("add_folder_button")
            ) {
                Icon(
                    if (rememberedFolderName != null) Icons.Default.FolderOpen else Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    tint = if (rememberedFolderName != null) skin.albumFrameColor else Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (rememberedFolderName != null) "Change Folder" else "Add Music Folder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Sync Device Storage Button
            OutlinedButton(
                onClick = onScanDevice,
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(skin.textCyanColor.copy(alpha = 0.6f))
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.textCyanColor),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("scan_device_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan Device", fontSize = 12.sp)
            }
        }

        // Status Toast Banner
        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(skin.surfaceColor)
                    .border(1.dp, skin.albumFrameColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage,
                        color = skin.albumFrameColor,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearStatus, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // EXTERNAL USB STORAGE / OTG SECTION
        UsbStorageSection(
            connectedDevices = connectedUsbDevices,
            usbStatusMessage = usbStatusMessage,
            skin = skin,
            onRefresh = onRefreshUsbDevices,
            onExplore = onOpenUsbExplorer,
            onScan = onScanUsbDevice,
            onSelectSafUsb = { folderPickerLauncher.launch(null) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search songs, artists, formats...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = skin.textCyanColor) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = skin.textCyanColor,
                unfocusedBorderColor = Color(0x33FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = skin.textCyanColor,
                focusedContainerColor = skin.surfaceColor,
                unfocusedContainerColor = skin.surfaceColor
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // SOURCE FILTER BAR (All Tracks / USB Only / Local Only)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalCount = remember(songs) { songs.size }
            val usbCount = remember(songs) { songs.count { it.isUsb || it.folderName?.startsWith("USB", ignoreCase = true) == true } }
            val localCount = remember(songs) { songs.count { !it.isUsb && it.folderName?.startsWith("USB", ignoreCase = true) != true } }

            FilterChip(
                selected = sourceFilter == LibrarySourceFilter.ALL,
                onClick = { onSourceFilterChange(LibrarySourceFilter.ALL) },
                label = { Text("All ($totalCount)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = skin.albumFrameColor,
                    selectedLabelColor = Color.Black,
                    containerColor = skin.surfaceColor,
                    labelColor = Color.LightGray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (sourceFilter == LibrarySourceFilter.ALL) skin.albumFrameColor else Color(0x33FFFFFF),
                    enabled = true,
                    selected = sourceFilter == LibrarySourceFilter.ALL
                ),
                modifier = Modifier.testTag("filter_all_tracks")
            )

            FilterChip(
                selected = sourceFilter == LibrarySourceFilter.USB_ONLY,
                onClick = { onSourceFilterChange(LibrarySourceFilter.USB_ONLY) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Usb,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (sourceFilter == LibrarySourceFilter.USB_ONLY) Color.Black else Color(0xFF00E676)
                    )
                },
                label = { Text("USB ($usbCount)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00E676),
                    selectedLabelColor = Color.Black,
                    containerColor = skin.surfaceColor,
                    labelColor = Color.LightGray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (sourceFilter == LibrarySourceFilter.USB_ONLY) Color(0xFF00E676) else Color(0x33FFFFFF),
                    enabled = true,
                    selected = sourceFilter == LibrarySourceFilter.USB_ONLY
                ),
                modifier = Modifier.testTag("filter_usb_tracks")
            )

            FilterChip(
                selected = sourceFilter == LibrarySourceFilter.LOCAL_ONLY,
                onClick = { onSourceFilterChange(LibrarySourceFilter.LOCAL_ONLY) },
                label = { Text("Local ($localCount)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = skin.textCyanColor,
                    selectedLabelColor = Color.Black,
                    containerColor = skin.surfaceColor,
                    labelColor = Color.LightGray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (sourceFilter == LibrarySourceFilter.LOCAL_ONLY) skin.textCyanColor else Color(0x33FFFFFF),
                    enabled = true,
                    selected = sourceFilter == LibrarySourceFilter.LOCAL_ONLY
                ),
                modifier = Modifier.testTag("filter_local_tracks")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Songs List
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rock_hand),
                        contentDescription = null,
                        tint = skin.albumFrameColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tracks found",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'Add Music Folder' to scan songs from any directory or subfolder.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("songs_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = songs,
                    key = { it.id },
                    contentType = { "song_item" }
                ) { song ->
                    val isCurrent = currentSong?.id == song.id
                    SongListItem(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying,
                        skin = skin,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    skin: PlayerSkinTheme,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bgColor = if (isCurrent) skin.surfaceColor else Color(0xFF0F0F13)
    val borderColor = if (isCurrent) skin.albumFrameColor else Color(0x1AFFFFFF)

    // Memoize the downsampled, hardware-accelerated image request per song to avoid
    // object allocation during fast LazyColumn scroll passes.
    val thumbnailRequest = remember(song.id, song.albumArtUriString) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isCurrent) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("song_item_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon / Album Art thumbnail / Equalizer indicator
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrent) skin.albumFrameColor else Color(0xFF1E1E26)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = thumbnailRequest,
                contentDescription = "${song.title} artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallback = painterResource(id = R.drawable.ic_rock_hand),
                error = painterResource(id = R.drawable.ic_rock_hand)
            )
            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = skin.albumFrameColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) skin.albumFrameColor else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    color = skin.textCyanColor.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (song.folderName != null) {
                    Text(
                        text = " • ${song.folderName}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Format tag and duration
        Column(horizontalAlignment = Alignment.End) {
            // Badges row (USB badge + Hi-Res / Format badge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.isUsb || song.folderName?.startsWith("USB", ignoreCase = true) == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.2f))
                            .border(0.8.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚡ USB",
                            color = Color(0xFF00E676),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (song.isHiRes) skin.playButtonColor.copy(alpha = 0.2f)
                            else Color(0x22FFFFFF)
                        )
                        .border(
                            0.8.dp,
                            if (song.isHiRes) skin.playButtonColor else Color(0x33FFFFFF),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (song.isHiRes) "HI-RES" else song.format,
                        color = if (song.isHiRes) skin.playButtonColor else Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = song.formattedDuration(),
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun LibraryDetailPane(
    currentSong: Song?,
    isPlaying: Boolean,
    skin: PlayerSkinTheme,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = skin.surfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, skin.albumFrameColor.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag("library_track_detail_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRACK INSPECTION",
                    color = skin.albumFrameColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                if (currentSong != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentSong.isUsb || currentSong.folderName?.startsWith("USB", ignoreCase = true) == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.25f))
                                    .border(1.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⚡ USB DRIVE",
                                    color = Color(0xFF00E676),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .border(1.dp, skin.albumFrameColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
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

            Spacer(modifier = Modifier.height(14.dp))

            if (currentSong != null) {
                // Album Art in Iconic Frame
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(skin.albumFrameColor)
                        .border(2.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF101014)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentSong)
                                .crossfade(150)
                                .build(),
                            contentDescription = currentSong.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentSong.title,
                    color = skin.textCyanColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentSong.artist,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = currentSong.album,
                    color = skin.textSecondaryColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Track specs chip row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF101014))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentSong.formattedDuration(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF101014))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentSong.format,
                            color = skin.albumFrameColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Play / Replay button
                Button(
                    onClick = { onSongClick(currentSong) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.albumFrameColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .fillMaxWidth(0.85f)
                        .testTag("detail_play_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rock_hand),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "Playing Now" else "Play Track",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Empty state for detail pane
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rock_hand),
                        contentDescription = null,
                        tint = skin.albumFrameColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Track Inspection",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select any song on the left to inspect metadata, format specs, and play.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun UsbStorageSection(
    connectedDevices: List<UsbStorageDevice>,
    usbStatusMessage: String?,
    skin: PlayerSkinTheme,
    onRefresh: () -> Unit,
    onExplore: (UsbStorageDevice) -> Unit,
    onScan: (UsbStorageDevice, Uri?) -> Unit,
    onSelectSafUsb: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = skin.surfaceColor),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (connectedDevices.isNotEmpty()) Color(0xFF00E676).copy(alpha = 0.8f) else skin.cardBorderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("usb_storage_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (connectedDevices.isNotEmpty()) Color(0xFF00E676).copy(alpha = 0.2f)
                                else skin.albumFrameColor.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Usb,
                            contentDescription = "USB OTG",
                            tint = if (connectedDevices.isNotEmpty()) Color(0xFF00E676) else skin.albumFrameColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "EXTERNAL USB STORAGE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (connectedDevices.isNotEmpty()) Color(0xFF00E676).copy(alpha = 0.25f)
                                        else Color(0x22FFFFFF)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (connectedDevices.isNotEmpty()) "${connectedDevices.size} READY" else "OTG READY",
                                    color = if (connectedDevices.isNotEmpty()) Color(0xFF00E676) else Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = if (connectedDevices.isNotEmpty())
                                "Connected USB drives detected and ready to explore"
                            else
                                "Connect a USB OTG drive or explore via system file picker",
                            color = skin.textSecondaryColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_refresh_usb_devices")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh USB devices",
                        tint = skin.albumFrameColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (connectedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    connectedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F0F13))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (device.isMounted) Color(0xFF00E676) else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = device.description,
                                        color = skin.textCyanColor,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    device.formattedCapacity?.let { cap ->
                                        Text(
                                            text = " • $cap",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onExplore(device) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(skin.albumFrameColor.copy(alpha = 0.7f))
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("btn_explore_usb_${device.id}")
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Explore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onScan(device, device.rootUri) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E676),
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("btn_scan_usb_${device.id}")
                                ) {
                                    Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.textCyanColor),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(skin.textCyanColor.copy(alpha = 0.5f))
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Detect USB", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onSelectSafUsb,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.albumFrameColor),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(skin.albumFrameColor.copy(alpha = 0.5f))
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(36.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Browse USB / OTG Drive", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

