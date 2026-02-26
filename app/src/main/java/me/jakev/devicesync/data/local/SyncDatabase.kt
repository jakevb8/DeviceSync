package me.jakev.devicesync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import me.jakev.devicesync.data.model.SyncStatus

@Database(
    entities = [SyncFileEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SyncStatusConverter::class)
abstract class SyncDatabase : RoomDatabase() {
    abstract fun syncFileDao(): SyncFileDao
}

class SyncStatusConverter {
    @TypeConverter
    fun fromStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
