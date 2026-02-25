package com.devicesync.app.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.devicesync.app.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncFileDao {

    @Query("SELECT * FROM sync_files WHERE syncPairId = :pairId ORDER BY fileName ASC")
    fun getFilesForPair(pairId: String): Flow<List<SyncFileEntity>>

    @Query("SELECT * FROM sync_files WHERE syncPairId = :pairId AND syncStatus = :status")
    fun getFilesByStatus(pairId: String, status: SyncStatus): Flow<List<SyncFileEntity>>

    @Query("SELECT * FROM sync_files WHERE localPath = :localPath LIMIT 1")
    suspend fun getFileByLocalPath(localPath: String): SyncFileEntity?

    @Query("SELECT * FROM sync_files WHERE syncPairId = :pairId AND syncStatus IN ('PENDING','MODIFIED','FAILED')")
    suspend fun getPendingFiles(pairId: String): List<SyncFileEntity>

    @Query("SELECT COUNT(*) FROM sync_files WHERE syncPairId = :pairId AND syncStatus = 'SYNCED'")
    fun getSyncedCount(pairId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_files WHERE syncPairId = :pairId")
    fun getTotalCount(pairId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: SyncFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(files: List<SyncFileEntity>)

    @Update
    suspend fun update(file: SyncFileEntity)

    @Query("UPDATE sync_files SET syncStatus = :status, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncStatus, syncedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sync_files SET syncStatus = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatusWithError(id: String, status: SyncStatus, error: String?)

    @Query("DELETE FROM sync_files WHERE syncPairId = :pairId")
    suspend fun deleteAllForPair(pairId: String)

    @Query("DELETE FROM sync_files WHERE id = :id")
    suspend fun deleteById(id: String)
}
