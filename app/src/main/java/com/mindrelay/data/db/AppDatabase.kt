package com.mindrelay.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.model.ConvertType
import com.mindrelay.data.model.EntryKind
import com.mindrelay.data.model.MemorySourceType
import com.mindrelay.data.model.MemoryType
import com.mindrelay.data.model.ProjectStatus

class Converters {
    @TypeConverter fun captureKindToStr(v: CaptureKind) = v.name
    @TypeConverter fun strToCaptureKind(v: String) = CaptureKind.valueOf(v)

    @TypeConverter fun memoryTypeToStr(v: MemoryType) = v.name
    @TypeConverter fun strToMemoryType(v: String) = MemoryType.valueOf(v)

    @TypeConverter fun projectStatusToStr(v: ProjectStatus) = v.name
    @TypeConverter fun strToProjectStatus(v: String) = ProjectStatus.valueOf(v)

    @TypeConverter fun entryKindToStr(v: EntryKind) = v.name
    @TypeConverter fun strToEntryKind(v: String) = EntryKind.valueOf(v)

    @TypeConverter fun sourceTypeToStr(v: MemorySourceType) = v.name
    @TypeConverter fun strToSourceType(v: String) = MemorySourceType.valueOf(v)

    @TypeConverter fun convertTypeToStr(v: ConvertType) = v.name
    @TypeConverter fun strToConvertType(v: String) = ConvertType.valueOf(v)
}

@Database(
    entities = [
        CaptureEntity::class,
        ProjectEntity::class,
        SessionEntity::class,
        SessionEntryEntity::class,
        MemoryEntity::class,
        TaskEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionEntryDao(): SessionEntryDao
    abstract fun entryDao(): SessionEntryDao
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao
}
