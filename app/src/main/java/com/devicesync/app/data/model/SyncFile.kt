package com.devicesync.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Tracks the sync state of an individual file within a SyncPair.
 * Stored in Firestore under users/{uid}/syncPairs/{pairId}/files/{fileId}
 * and mirrored locally in Room.
 */
@Parcelize
data class SyncFile(
    val id: String = "",
    val syncPairId: String = "",
    val localPath: String = "",
    val remotePath: String = "",     // Firebase Storage path
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val checksum: String = "",       // MD5 hash for change detection
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val syncedAt: Long = 0L,
    val errorMessage: String? = null
) : Parcelable

enum class SyncStatus {
    PENDING,        // Queued, not yet uploaded
    SYNCING,        // Currently uploading
    SYNCED,         // Successfully synced
    MODIFIED,       // Local file changed since last sync
    FAILED,         // Last sync attempt failed
    DELETED         // File deleted locally
}
