package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.usb.UsbDeviceManager
import com.example.data.usb.UsbFileItem
import com.example.data.usb.UsbStorageDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsbDeviceTest {

    @Test
    fun testUsbStorageDeviceProperties() {
        val device = UsbStorageDevice(
            id = "usb_drive_1",
            name = "SanDisk Ultra 64GB",
            description = "External USB Flash Drive",
            isRemovable = true,
            state = "mounted",
            path = "/storage/USB_DISK",
            isMounted = true,
            totalBytes = 64L * 1024L * 1024L * 1024L,
            freeBytes = 32L * 1024L * 1024L * 1024L
        )

        assertEquals("usb_drive_1", device.id)
        assertEquals("SanDisk Ultra 64GB", device.name)
        assertTrue(device.isMounted)
        assertNotNull(device.formattedCapacity)
        assertTrue(device.formattedCapacity?.contains("GB") == true)
        assertTrue(device.formattedFreeSpace?.contains("GB") == true)
    }

    @Test
    fun testUsbFileItemProperties() {
        val audioFile = UsbFileItem(
            name = "Back In Black.flac",
            isDirectory = false,
            pathOrDocId = "/storage/USB_DISK/Rock/Back In Black.flac",
            uriString = "file:///storage/USB_DISK/Rock/Back In Black.flac",
            sizeBytes = 25_000_000L,
            mimeType = "audio/flac",
            format = "FLAC"
        )

        assertEquals("Back In Black.flac", audioFile.name)
        assertFalse(audioFile.isDirectory)
        assertTrue(audioFile.formattedSize.contains("MB"))

        val folderItem = UsbFileItem(
            name = "Classic Rock",
            isDirectory = true,
            pathOrDocId = "/storage/USB_DISK/Rock/Classic Rock",
            uriString = "file:///storage/USB_DISK/Rock/Classic Rock",
            sizeBytes = 0L,
            childCount = 5
        )

        assertTrue(folderItem.isDirectory)
        assertEquals("5 items", folderItem.formattedSize)

        val nonAudioFile = UsbFileItem(
            name = "cover.jpg",
            isDirectory = false,
            pathOrDocId = "/storage/USB_DISK/Rock/cover.jpg",
            uriString = "file:///storage/USB_DISK/Rock/cover.jpg",
            sizeBytes = 200_000L,
            mimeType = "image/jpeg",
            format = "IMAGE"
        )

        assertFalse(nonAudioFile.isDirectory)
        assertTrue(nonAudioFile.formattedSize.contains("KB"))
    }

    @Test
    fun testUsbDeviceManagerInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val usbManager = UsbDeviceManager(context)

        // Verify connectedUsbDevices state flow
        val devices = usbManager.connectedUsbDevices.value
        assertNotNull(devices)

        // Verify refresh
        usbManager.refreshDevices()
        assertNotNull(usbManager.connectedUsbDevices.value)

        val statusFlow = usbManager.usbStatusMessage.value
        assertTrue(statusFlow == null || statusFlow.isNotEmpty())
    }
}
