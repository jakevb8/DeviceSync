package me.jakev.devicesync.data.repository

import me.jakev.devicesync.data.local.SyncDatabase
import me.jakev.devicesync.data.local.SyncFileEntity
import me.jakev.devicesync.data.model.SyncFile
import me.jakev.devicesync.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncFileRepository @Inject constructor(
    private val db: SyncDatabase
) {
    private val dao = db.syncFileDao()

    fun getFilesForPair(pairId: String): Flow<List<SyncFile>> =
        dao.getFilesForPair(pairId).map { list -> list.map { it.toDomain() } }

    fun getFilesByStatus(pairId: String, status: SyncStatus): Flow<List<SyncFile>> =
        dao.getFilesByStatus(pairId, status).map { list -> list.map { it.toDomain() } }

    suspend fun getFileByLocalPath(localPath: String): SyncFile? =
        dao.getFileByLocalPath(localPath)?.toDomain()

    suspend fun getPendingFiles(pairId: String): List<SyncFile> =
        dao.getPendingFiles(pairId).map { it.toDomain() }

    fun getSyncedCount(pairId: String): Flow<Int> = dao.getSyncedCount(pairId)
    fun getTotalCount(pairId: String): Flow<Int> = dao.getTotalCount(pairId)

    suspend fun upsert(file: SyncFile) = dao.upsert(file.toEntity())
    suspend fun upsertAll(files: List<SyncFile>) = dao.upsertAll(files.map { it.toEntity() })
    suspend fun updateStatus(id: String, status: SyncStatus, syncedAt: Long = System.currentTimeMillis()) =
        dao.updateStatus(id, status, syncedAt)
    suspend fun updateStatusWithError(id: String, status: SyncStatus, error: String?) =
        dao.updateStatusWithError(id, status, error)
    suspend fun deleteAllForPair(pairId: String) = dao.deleteAllForPair(pairId)

    private fun SyncFileEntity.toDomain() = SyncFile(
        id = id, syncPairId = syncPairId, localPath = localPath, remotePath = remotePath,
        fileName = fileName, fileSizeBytes = fileSizeBytes, lastModified = lastModified,
        checksum = checksum, syncStatus = syncStatus, syncedAt = syncedAt,
        errorMessage = errorMessage
    )

    private fun SyncFile.toEntity() = SyncFileEntity(
        id = id, syncPairId = syncPairId, localPath = localPath, remotePath = remotePath,
        fileName = fileName, fileSizeBytes = fileSizeBytes, lastModified = lastModified,
        checksum = checksum, syncStatus = syncStatus, syncedAt = syncedAt,
        errorMessage = errorMessage
    )
}
