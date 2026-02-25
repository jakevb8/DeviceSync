package com.devicesync.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.devicesync.app.data.model.SyncFile
import com.devicesync.app.data.model.SyncPair
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPairRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    /**
     * Observe all sync pairs belonging to this user (both parent & child registrations).
     * Firestore real-time updates ensure any new device login sees the pairs immediately.
     */
    fun observeSyncPairs(uid: String): Flow<List<SyncPair>> = callbackFlow {
        val listener = userDoc(uid).collection("syncPairs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing sync pairs")
                    return@addSnapshotListener
                }
                val pairs = snapshot?.toObjects<SyncPair>() ?: emptyList()
                trySend(pairs)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observe synced files for a specific pair.
     */
    fun observeFilesForPair(uid: String, pairId: String): Flow<List<SyncFile>> = callbackFlow {
        val listener = userDoc(uid).collection("syncPairs")
            .document(pairId)
            .collection("files")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing files for pair $pairId")
                    return@addSnapshotListener
                }
                val files = snapshot?.toObjects<SyncFile>() ?: emptyList()
                trySend(files)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Register a new parent folder (called on the parent device).
     */
    suspend fun registerParentPair(uid: String, pair: SyncPair): Result<SyncPair> {
        return try {
            val ref = userDoc(uid).collection("syncPairs").document()
            val newPair = pair.copy(id = ref.id, ownerUid = uid)
            ref.set(newPair).await()
            Result.success(newPair)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register parent pair")
            Result.failure(e)
        }
    }

    /**
     * Link a child device/folder to an existing parent pair.
     */
    suspend fun linkChildToPair(uid: String, pairId: String, childDeviceName: String,
                                childDeviceId: String, childFolderPath: String): Result<Unit> {
        return try {
            userDoc(uid).collection("syncPairs").document(pairId)
                .update(
                    "childDeviceName", childDeviceName,
                    "childDeviceId", childDeviceId,
                    "childFolderPath", childFolderPath
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to link child to pair $pairId")
            Result.failure(e)
        }
    }

    /**
     * Update the lastSyncedAt timestamp on a pair.
     */
    suspend fun updateLastSynced(uid: String, pairId: String, timestamp: Long = System.currentTimeMillis()) {
        try {
            userDoc(uid).collection("syncPairs").document(pairId)
                .update("lastSyncedAt", timestamp).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to update lastSyncedAt for pair $pairId")
        }
    }

    /**
     * Write/update file metadata to Firestore so parent device can see sync state.
     */
    suspend fun upsertFileRecord(uid: String, pairId: String, file: SyncFile) {
        try {
            userDoc(uid).collection("syncPairs")
                .document(pairId)
                .collection("files")
                .document(file.id)
                .set(file).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert file record ${file.id}")
        }
    }

    suspend fun deletePair(uid: String, pairId: String): Result<Unit> {
        return try {
            userDoc(uid).collection("syncPairs").document(pairId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
