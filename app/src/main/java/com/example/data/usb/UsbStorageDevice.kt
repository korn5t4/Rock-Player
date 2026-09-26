package com.example.data.usb

import android.net.Uri

/**
 * Represents a detected USB storage device or external removable drive.
 */
data class UsbStorageDevice(
    val id: String,
    val name: String,
    val description: String,
    val isRemovable: Boolean,
    val state: String,
    val path: String? = null,
    val uuid: String? = null,
    val totalBytes: Long? = null,
    val freeBytes: Long? = null,
    val isMounted: Boolean = true,
    val rootUri: Uri? = null,
    val isUsbMassStorage: Boolean = true
) {
    val formattedCapacity: String?
        get() {
            if (totalBytes == null || totalBytes <= 0) return null
            val gb = totalBytes.toDouble() / (1024 * 1024 * 1024)
            return if (gb >= 1.0) "%.1f GB".format(gb) else "%d MB".format(totalBytes / (1024 * 1024))
        }

    val formattedFreeSpace: String?
        get() {
            if (freeBytes == null || freeBytes <= 0) return null
            val gb = freeBytes.toDouble() / (1024 * 1024 * 1024)
            return if (gb >= 1.0) "%.1f GB free".format(gb) else "%d MB free".format(freeBytes / (1024 * 1024))
        }
}

/**
 * Represents an entry (file or folder) when exploring a USB device.
 */
data class UsbFileItem(
    val name: String,
    val isDirectory: Boolean,
    val pathOrDocId: String,
    val uriString: String,
    val sizeBytes: Long = 0,
    val mimeType: String? = null,
    val format: String = "AUDIO",
    val childCount: Int? = null
) {
    val formattedSize: String
        get() {
            if (isDirectory) return childCount?.let { "$it items" } ?: "Folder"
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1.0) "%.1f MB".format(mb) else "%d KB".format(sizeBytes / 1024)
        }
}
