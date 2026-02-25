package com.devicesync.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a registered sync pair: a "child" folder syncs TO a "parent" folder/device.
 * Stored in Firestore under users/{uid}/syncPairs/{pairId}
 */
@Parcelize
data class SyncPair(
    val id: String = "",
    val ownerUid: String = "",
    val parentDeviceName: String = "",
    val parentDeviceId: String = "",
    val parentFolderPath: String = "",   // Remote storage path prefix
    val childFolderPath: String = "",    // Local absolute path on child device
    val childDeviceName: String = "",
    val childDeviceId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0L,
    val isActive: Boolean = true,
    val role: DeviceRole = DeviceRole.CHILD   // Role of the current device in this pair
) : Parcelable

enum class DeviceRole { PARENT, CHILD }
