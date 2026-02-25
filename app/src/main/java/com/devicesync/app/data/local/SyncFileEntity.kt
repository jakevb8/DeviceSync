package com.devicesync.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devicesync.app.data.model.SyncStatus

@Entity(
    tableName = "sync_files",
    indices = [
        Index(value = ["syncPairId"]),
        Index(value = ["localPath"], unique = true),
        Index(value = ["syncStatus"])
    ]
)
data class SyncFileEntity(
    @PrimaryKey val id: String,
    val syncPairId: String,
    val localPath: String,
    val remotePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val lastModified: Long,
    val checksum: String,
    val syncStatus: SyncStatus,
    val syncedAt: Long,
    val errorMessage: String?
)
