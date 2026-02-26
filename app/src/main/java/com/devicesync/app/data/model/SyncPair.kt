package com.devicesync.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a registered sync pair stored in Firestore (settings/config only).
 * Files are NOT stored in Firestore — they transfer directly over local WiFi
 * from the child device HTTP server to the parent device.
 */
@Parcelize
data class SyncPair(
    val id: String = "",
    val ownerUid: String = "",
    val parentDeviceName: String = "",
    val parentDeviceId: String = "",
    val parentFolderPath: String = "",      // Local path on the parent device
    val childFolderPath: String = "",       // Local path on the child device
    val childDeviceName: String = "",
    val childDeviceId: String = "",
    val childIpAddress: String = "",        // LAN IP of child device (updated on each session)
    val childSyncPort: Int = 8765,          // Port the child's HTTP file server listens on
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0L,
    val isActive: Boolean = true,
    val role: DeviceRole = DeviceRole.CHILD
) : Parcelable

enum class DeviceRole { PARENT, CHILD }
