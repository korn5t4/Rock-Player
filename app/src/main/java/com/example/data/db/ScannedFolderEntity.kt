package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a scanned and authorized folder or drive.
 */
@Entity(tableName = "scanned_folders")
data class ScannedFolderEntity(
    @PrimaryKey
    val folderUri: String,
    val folderName: String,
    val lastScannedTimestamp: Long = System.currentTimeMillis(),
    val trackCount: Int = 0,
    val isUsb: Boolean = false
)
