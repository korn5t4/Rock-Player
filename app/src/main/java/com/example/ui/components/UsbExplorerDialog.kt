package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.usb.UsbFileItem
import com.example.data.usb.UsbStorageDevice
import com.example.ui.theme.PlayerSkinTheme
import kotlinx.coroutines.launch

/**
 * Interactive file & folder explorer for external USB OTG storage devices.
 * Allows drilling into subdirectories, playing audio files directly, and scanning folders.
 */
@Composable
fun UsbExplorerDialog(
    device: UsbStorageDevice,
    skin: PlayerSkinTheme,
    onDismiss: () -> Unit,
    onExploreDirectory: suspend (device: UsbStorageDevice, currentPath: String?, treeUri: Uri?) -> List<UsbFileItem>,
    onPlayFileDirect: (item: UsbFileItem, device: UsbStorageDevice) -> Unit,
    onScanFolder: (device: UsbStorageDevice, treeUri: Uri?) -> Unit,
    onRequestSafPermission: ((device: UsbStorageDevice) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    val fileItems = remember { mutableStateListOf<UsbFileItem>() }

    // Navigation breadcrumbs stack: Pair(displayName, pathOrDocId)
    val navStack = remember {
        mutableStateListOf(Pair(device.name, device.path))
    }

    fun loadCurrentDirectory() {
        coroutineScope.launch {
            isLoading = true
            try {
                val currentPath = navStack.lastOrNull()?.second
                val items = onExploreDirectory(device, currentPath, device.rootUri)
                fileItems.clear()
                fileItems.addAll(items)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(device, navStack.size) {
        loadCurrentDirectory()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, skin.albumFrameColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            color = skin.backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(skin.albumFrameColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Usb,
                                contentDescription = "USB Device",
                                tint = skin.albumFrameColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = device.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (device.isMounted) "MOUNTED • USB OTG" else "DISCONNECTED",
                                    color = skin.textCyanColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                device.formattedCapacity?.let { cap ->
                                    Text(
                                        text = " • $cap",
                                        color = skin.textSecondaryColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_usb_explorer")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Breadcrumb bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(skin.surfaceColor)
                        .border(1.dp, skin.cardBorderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (navStack.size > 1) {
                        IconButton(
                            onClick = {
                                if (navStack.size > 1) {
                                    navStack.removeAt(navStack.lastIndex)
                                    loadCurrentDirectory()
                                }
                            },
                            modifier = Modifier.size(28.dp).testTag("btn_usb_nav_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Up One Level",
                                tint = skin.albumFrameColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            tint = skin.albumFrameColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = navStack.joinToString(" / ") { it.first },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { loadCurrentDirectory() },
                        modifier = Modifier.size(28.dp).testTag("btn_usb_refresh")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content list (Folders & Audio files)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F0F13))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = skin.albumFrameColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Reading USB storage...",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (fileItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No audio tracks or subfolders found here",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Supports MP3, FLAC, WAV, AAC, OGG, M4A",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                if (onRequestSafPermission != null) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { onRequestSafPermission(device) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = skin.albumFrameColor,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Authorize Full USB Storage Access", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("usb_file_list")
                        ) {
                            items(fileItems, key = { it.pathOrDocId }) { item ->
                                UsbFileItemRow(
                                    item = item,
                                    skin = skin,
                                    onFolderClick = {
                                        navStack.add(Pair(item.name, item.pathOrDocId))
                                        loadCurrentDirectory()
                                    },
                                    onPlayClick = {
                                        onPlayFileDirect(item, device)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Bar: Scan / Import All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.5f))
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Done", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onScanFolder(device, device.rootUri)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("btn_usb_import_all"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = skin.albumFrameColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            Icons.Default.Usb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan & Import USB Music",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsbFileItemRow(
    item: UsbFileItem,
    skin: PlayerSkinTheme,
    onFolderClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (item.isDirectory) Color(0x18FFFFFF) else Color(0x0CFFFFFF))
            .clickable {
                if (item.isDirectory) onFolderClick() else onPlayClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isDirectory) skin.textCyanColor.copy(alpha = 0.15f)
                        else skin.albumFrameColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = if (item.isDirectory) skin.textCyanColor else skin.albumFrameColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.isDirectory) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(skin.albumFrameColor.copy(alpha = 0.25f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.format,
                                color = skin.albumFrameColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.formattedSize,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!item.isDirectory) {
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(skin.playButtonColor)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play Direct",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
