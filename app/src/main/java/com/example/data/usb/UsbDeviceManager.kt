package com.example.data.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import com.example.data.model.Song
import com.example.data.repository.MusicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Manages detection, browsing, and scanning of external USB storage devices (USB OTG flash drives,
 * external drives, and removable storage).
 */
class UsbDeviceManager(private val context: Context) {

    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val _connectedUsbDevices = MutableStateFlow<List<UsbStorageDevice>>(emptyList())
    val connectedUsbDevices: StateFlow<List<UsbStorageDevice>> = _connectedUsbDevices.asStateFlow()

    private val _usbStatusMessage = MutableStateFlow<String?>(null)
    val usbStatusMessage: StateFlow<String?> = _usbStatusMessage.asStateFlow()

    private var receiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val action = intent.action ?: return
            when (action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val devName = device?.productName ?: "USB Device"
                    _usbStatusMessage.value = "⚡ USB Device Attached: $devName"
                    refreshDevices()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val devName = device?.productName ?: "USB Device"
                    _usbStatusMessage.value = "USB Device Disconnected: $devName"
                    refreshDevices()
                }
                Intent.ACTION_MEDIA_MOUNTED -> {
                    val uri = intent.data
                    _usbStatusMessage.value = "⚡ External Storage Mounted: ${uri?.path ?: "USB Drive"}"
                    refreshDevices()
                }
                Intent.ACTION_MEDIA_UNMOUNTED, Intent.ACTION_MEDIA_REMOVED, Intent.ACTION_MEDIA_EJECT -> {
                    _usbStatusMessage.value = "External Storage Unmounted"
                    refreshDevices()
                }
            }
        }
    }

    init {
        registerUsbReceiver()
        refreshDevices()
    }

    fun registerUsbReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addDataScheme("file")
            }
            // For standard USB attach/detach actions without data scheme:
            val usbFilter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                usbFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
            receiverRegistered = true
        } catch (e: Exception) {
            // Receiver registration fallback
            try {
                val simpleFilter = IntentFilter().apply {
                    addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                    addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                }
                context.registerReceiver(usbReceiver, simpleFilter)
                receiverRegistered = true
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    fun unregisterUsbReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            receiverRegistered = false
        }
    }

    /**
     * Queries the system for all connected removable USB drives and external volumes.
     */
    fun refreshDevices() {
        val result = mutableListOf<UsbStorageDevice>()

        // 1. Check StorageManager storage volumes
        storageManager?.storageVolumes?.forEach { volume ->
            val isRemovable = volume.isRemovable
            val state = volume.state

            // Detect removable external volumes (USB drives, OTG flash drives, external SD cards)
            if (isRemovable || (!volume.isPrimary && state == Environment.MEDIA_MOUNTED)) {
                val description = volume.getDescription(context).takeIf { it.isNotBlank() } ?: "USB Storage Drive"
                val uuid = volume.uuid

                var dirPath: String? = null
                var totalBytes: Long? = null
                var freeBytes: Long? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val dir = volume.directory
                        dirPath = dir?.absolutePath
                        if (dir != null && dir.exists() && dir.canRead()) {
                            totalBytes = dir.totalSpace
                            freeBytes = dir.freeSpace
                        }
                    } catch (e: Exception) {}
                }

                // Fallback directory search for /storage/UUID
                if (dirPath == null && uuid != null) {
                    val potentialDir = File("/storage/$uuid")
                    if (potentialDir.exists()) {
                        dirPath = potentialDir.absolutePath
                        if (potentialDir.canRead()) {
                            totalBytes = potentialDir.totalSpace
                            freeBytes = potentialDir.freeSpace
                        }
                    }
                }

                result.add(
                    UsbStorageDevice(
                        id = uuid ?: "vol_${description.hashCode()}",
                        name = description,
                        description = if (isRemovable) "Removable USB Storage" else "External Storage Volume",
                        isRemovable = isRemovable,
                        state = state,
                        path = dirPath,
                        uuid = uuid,
                        totalBytes = totalBytes,
                        freeBytes = freeBytes,
                        isMounted = state == Environment.MEDIA_MOUNTED,
                        isUsbMassStorage = true
                    )
                )
            }
        }

        // 2. Cross-reference with UsbManager connected USB devices
        try {
            val deviceMap = usbManager?.deviceList
            deviceMap?.values?.forEach { usbDev ->
                var hasMassStorage = false
                for (i in 0 until usbDev.interfaceCount) {
                    val iface = usbDev.getInterface(i)
                    if (iface.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) {
                        hasMassStorage = true
                        break
                    }
                }

                val devName = usbDev.productName ?: usbDev.manufacturerName ?: "USB OTG Device"
                val existing = result.find { it.name.contains(devName, ignoreCase = true) }
                if (existing == null) {
                    result.add(
                        UsbStorageDevice(
                            id = "usb_hw_${usbDev.deviceId}",
                            name = devName,
                            description = if (hasMassStorage) "USB Flash Drive (Mass Storage)" else "USB OTG Device",
                            isRemovable = true,
                            state = if (hasMassStorage) Environment.MEDIA_MOUNTED else "Connected",
                            path = null,
                            uuid = null,
                            totalBytes = null,
                            freeBytes = null,
                            isMounted = true,
                            isUsbMassStorage = hasMassStorage
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _connectedUsbDevices.value = result
    }

    /**
     * Builds an intent to launch the Storage Access Framework (SAF) document tree picker
     * pre-targeted at a specific USB volume, if supported.
     */
    fun createUsbVolumeAccessIntent(volumeId: String?): Intent {
        val volume = storageManager?.storageVolumes?.find { it.uuid != null && it.uuid.equals(volumeId, ignoreCase = true) }
        if (volume != null) {
            try {
                return volume.createOpenDocumentTreeIntent()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
    }

    /**
     * Explores files and folders inside a USB storage device.
     * Can explore via direct File API (if accessible) or via SAF treeUri.
     */
    suspend fun exploreDirectory(
        device: UsbStorageDevice,
        currentPath: String? = null,
        treeUri: Uri? = null
    ): List<UsbFileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<UsbFileItem>()

        // 1. Try direct File reading if device directory is readable
        val targetPath = currentPath ?: device.path
        if (targetPath != null) {
            val dir = File(targetPath)
            if (dir.exists() && dir.canRead()) {
                val files = dir.listFiles()
                if (files != null) {
                    files.forEach { file ->
                        val isDir = file.isDirectory
                        val name = file.name
                        if (!name.startsWith(".")) {
                            if (isDir) {
                                val children = file.listFiles()?.size
                                items.add(
                                    UsbFileItem(
                                        name = name,
                                        isDirectory = true,
                                        pathOrDocId = file.absolutePath,
                                        uriString = Uri.fromFile(file).toString(),
                                        childCount = children
                                    )
                                )
                            } else if (MusicScanner.isSupportedAudioFile(name)) {
                                val ext = name.substringAfterLast('.', "").uppercase(Locale.ROOT)
                                items.add(
                                    UsbFileItem(
                                        name = name,
                                        isDirectory = false,
                                        pathOrDocId = file.absolutePath,
                                        uriString = Uri.fromFile(file).toString(),
                                        sizeBytes = file.length(),
                                        format = if (ext.isNotBlank()) ext else "AUDIO"
                                    )
                                )
                            }
                        }
                    }
                    return@withContext items.sortedWith(
                        compareByDescending<UsbFileItem> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
                    )
                }
            }
        }

        // 2. Fallback to SAF DocumentsContract if treeUri is provided
        if (treeUri != null) {
            try {
                val parentDocId = if (currentPath != null && currentPath != targetPath) {
                    currentPath
                } else {
                    try {
                        DocumentsContract.getTreeDocumentId(treeUri)
                    } catch (e: Exception) {
                        DocumentsContract.getDocumentId(treeUri)
                    }
                }

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                )

                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: "Unknown"
                        val mime = cursor.getString(mimeCol) ?: ""
                        val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
                        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR

                        val fileDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        if (!name.startsWith(".")) {
                            if (isDir) {
                                items.add(
                                    UsbFileItem(
                                        name = name,
                                        isDirectory = true,
                                        pathOrDocId = docId,
                                        uriString = fileDocUri.toString(),
                                        mimeType = mime
                                    )
                                )
                            } else if (MusicScanner.isSupportedAudioFile(name, mime)) {
                                val ext = name.substringAfterLast('.', "").uppercase(Locale.ROOT)
                                items.add(
                                    UsbFileItem(
                                        name = name,
                                        isDirectory = false,
                                        pathOrDocId = docId,
                                        uriString = fileDocUri.toString(),
                                        sizeBytes = size,
                                        mimeType = mime,
                                        format = if (ext.isNotBlank()) ext else "AUDIO"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        items.sortedWith(
            compareByDescending<UsbFileItem> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    /**
     * Recursively scans audio tracks from a USB device directly or via SAF.
     */
    suspend fun scanUsbDeviceTracks(
        device: UsbStorageDevice,
        treeUri: Uri? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        // 1. Direct File-system fast scan if directory accessible
        val rootPath = device.path
        if (rootPath != null) {
            val rootDir = File(rootPath)
            if (rootDir.exists() && rootDir.canRead()) {
                scanFileRecursively(rootDir, songs, device.name, maxDepth = 6, currentDepth = 0)
                if (songs.isNotEmpty()) {
                    return@withContext songs
                }
            }
        }

        // 2. SAF Tree scan if URI available
        if (treeUri != null) {
            val scanned = MusicScanner.scanDocumentTreeUri(context, treeUri)
            val usbTagged = scanned.map {
                it.copy(
                    isUsb = true,
                    folderName = "USB: ${device.name}"
                )
            }
            return@withContext usbTagged
        }

        songs
    }

    private fun scanFileRecursively(
        folder: File,
        outList: MutableList<Song>,
        usbDeviceName: String,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth) return
        val children = folder.listFiles() ?: return

        for (file in children) {
            if (file.name.startsWith(".")) continue
            if (file.isDirectory) {
                scanFileRecursively(file, outList, usbDeviceName, maxDepth, currentDepth + 1)
            } else if (MusicScanner.isSupportedAudioFile(file.name)) {
                val ext = file.name.substringAfterLast('.', "").uppercase(Locale.ROOT)
                val isHiRes = ext == "FLAC" || ext == "WAV"
                val (artist, title) = MusicScanner.fastParseTrackDetails(file.name, folder.name)

                outList.add(
                    Song(
                        id = "usb_${file.absolutePath.hashCode()}",
                        title = title,
                        artist = artist,
                        album = folder.name,
                        durationMs = 210000L,
                        uriString = Uri.fromFile(file).toString(),
                        format = ext,
                        isHiRes = isHiRes,
                        folderName = "USB: $usbDeviceName",
                        bitrateKbps = if (isHiRes) 1411 else 320,
                        sampleRateHz = if (isHiRes) 48000 else 44100,
                        isUsb = true
                    )
                )
            }
        }
    }
}
