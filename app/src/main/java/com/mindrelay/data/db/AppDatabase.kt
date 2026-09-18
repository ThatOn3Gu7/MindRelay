package com.mindrelay.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.model.ConvertType
import com.mindrelay.data.model.EntryKind
import com.mindrelay.data.model.MemorySourceType
import com.mindrelay.data.model.MemoryType
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.data.model.SessionStatus

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

    @TypeConverter fun sessionStatusToStr(v: SessionStatus) = v.name
    @TypeConverter fun strToSessionStatus(v: String) = SessionStatus.valueOf(v)
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
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionEntryDao(): SessionEntryDao
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao

    /** Alias used by the backup layer; Room allows only one abstract accessor per DAO. */
    fun entryDao(): SessionEntryDao = sessionEntryDao()

    companion object {
        /**
         * Deliberate, data-preserving migration strategy. Never falls back to
         * destructive migration: a personal-memory app must not lose data on
         * upgrade. Handwritten (rather than an auto-migration) because
         * `exportSchema` is off and every change is known.
         *
 *         v1 → v2 adds foreign keys + indices. SQLite cannot attach constraints
        *         with ALTER TABLE, so the five referencing tables are rebuilt with the
        *         standard 12-step pattern (rename → create → copy → drop) inside the
        *         migration transaction; every column type and order is preserved while
        *         the new FOREIGN KEY clauses are introduced.
        *
        *         v2 → v3 adds the persisted `sessions.displayNumber` column
        *         (backfilled 1..N per project by creation order) and the
        *         composite unique index that keeps session numbers unambiguous.
        */
        val MIGRATIONS: Array<Migration> = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Normalize legacy status strings to the typed enum names
                    // before the DAO reads them through the SessionStatus converter.
                    db.execSQL("UPDATE sessions SET status = 'ACTIVE' WHERE status = 'Active'")
                    db.execSQL("UPDATE sessions SET status = 'COMPLETED' WHERE status = 'Completed'")

                    // Reference cleanup for the new foreign keys. The pre-FK
                    // schema held links as plain longs, so dangling values are
                    // cleared (or their rows removed) before constraints land.
                    db.execSQL("UPDATE captures SET projectId = NULL WHERE projectId IS NOT NULL AND projectId NOT IN (SELECT id FROM projects)")
                    db.execSQL("UPDATE captures SET linkedSessionId = NULL WHERE linkedSessionId IS NOT NULL AND linkedSessionId NOT IN (SELECT id FROM sessions)")
                    db.execSQL("UPDATE memories SET projectId = NULL WHERE projectId IS NOT NULL AND projectId NOT IN (SELECT id FROM projects)")
                    db.execSQL("UPDATE memories SET sourceSessionId = NULL WHERE sourceSessionId IS NOT NULL AND sourceSessionId NOT IN (SELECT id FROM sessions)")
                    db.execSQL("UPDATE memories SET sourceCaptureId = NULL WHERE sourceCaptureId IS NOT NULL AND sourceCaptureId NOT IN (SELECT id FROM captures)")
                    db.execSQL("DELETE FROM tasks WHERE captureId IS NOT NULL AND captureId NOT IN (SELECT id FROM captures)")
                    db.execSQL("UPDATE tasks SET projectId = NULL WHERE projectId IS NOT NULL AND projectId NOT IN (SELECT id FROM projects)")
                    db.execSQL("DELETE FROM session_entries WHERE sessionId IS NOT NULL AND sessionId NOT IN (SELECT id FROM sessions)")
                    db.execSQL("DELETE FROM sessions WHERE projectId IS NOT NULL AND projectId NOT IN (SELECT id FROM projects)")

                    db.rebuildSessions()
                    db.rebuildSessionEntries()
                    db.rebuildCaptures()
                    db.rebuildMemories()
                    db.rebuildTasks()

                    // Declared indices (projects keeps its original columns and
                    // only gains indices, which apply in place).
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_lastWorkedAt` ON `projects` (`lastWorkedAt`)")
                }
            },
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Persisted per-project display number, so a session's label
                    // survives the deletion of earlier sessions.
                    db.execSQL("ALTER TABLE `sessions` ADD COLUMN `displayNumber` INTEGER NOT NULL DEFAULT 0")

                    // Backfill in creation order without window functions (which
                    // aren't available on every platform SQLite below API 30):
                    // 1..N per project ordered by startedAt then id.
                    db.execSQL(
                        "UPDATE `sessions` SET `displayNumber` = (" +
                            "SELECT COUNT(*) FROM `sessions` `s2` " +
                            "WHERE `s2`.`projectId` = `sessions`.`projectId` " +
                            "AND (`s2`.`startedAt` < `sessions`.`startedAt` " +
                            "OR (`s2`.`startedAt` = `sessions`.`startedAt` AND `s2`.`id` <= `sessions`.`id`))" +
                            ")"
                    )

                    // Schema guard for display numbers: at most one session per
                    // project may hold a given number, so "Session N" labels are
                    // unambiguous. Index name/columns/order must match Room's
                    // generated identity hash, so they mirror the @Entity Index.
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_sessions_projectId_displayNumber` " +
                            "ON `sessions` (`projectId`, `displayNumber`)"
                    )
                }
            },
        )
    }
}

/** Standard SQLite 12-step table rebuild that preserves rows while changing constraints. */
private fun SupportSQLiteDatabase.rebuildTable(table: String, targetSql: String, columns: List<String>) {
    val colList = columns.joinToString(", ") { "`$it`" }
    execSQL("ALTER TABLE `$table` RENAME TO `${table}_old`")
    execSQL(targetSql)
    execSQL("INSERT INTO `$table` ($colList) SELECT $colList FROM `${table}_old`")
    execSQL("DROP TABLE `${table}_old`")
}

private fun SupportSQLiteDatabase.rebuildSessions() {
    rebuildTable(
        table = "sessions",
        targetSql = "CREATE TABLE IF NOT EXISTS `sessions` (" +
            "`id` INTEGER NOT NULL, " +
            "`projectId` INTEGER NOT NULL, " +
            "`title` TEXT NOT NULL, " +
            "`status` TEXT NOT NULL, " +
            "`startedAt` INTEGER NOT NULL, " +
            "`endedAt` INTEGER, " +
            "`completed` TEXT NOT NULL, " +
            "`discoveries` TEXT NOT NULL, " +
            "`unresolved` TEXT NOT NULL, " +
            "`currentNextAction` TEXT NOT NULL, " +
            "PRIMARY KEY(`id`), " +
            "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        columns = listOf("id", "projectId", "title", "status", "startedAt", "endedAt", "completed", "discoveries", "unresolved", "currentNextAction"),
    )
    execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_projectId` ON `sessions` (`projectId`)")
}

private fun SupportSQLiteDatabase.rebuildSessionEntries() {
    rebuildTable(
        table = "session_entries",
        targetSql = "CREATE TABLE IF NOT EXISTS `session_entries` (" +
            "`id` INTEGER NOT NULL, " +
            "`sessionId` INTEGER NOT NULL, " +
            "`text` TEXT NOT NULL, " +
            "`kind` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`), " +
            "FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        columns = listOf("id", "sessionId", "text", "kind", "createdAt"),
    )
    execSQL("CREATE INDEX IF NOT EXISTS `index_session_entries_sessionId` ON `session_entries` (`sessionId`)")
}

private fun SupportSQLiteDatabase.rebuildCaptures() {
    rebuildTable(
        table = "captures",
        targetSql = "CREATE TABLE IF NOT EXISTS `captures` (" +
            "`id` INTEGER NOT NULL, " +
            "`text` TEXT NOT NULL, " +
            "`detail` TEXT NOT NULL, " +
            "`kind` TEXT NOT NULL, " +
            "`isVoice` INTEGER NOT NULL, " +
            "`projectId` INTEGER, " +
            "`linkedSessionId` INTEGER, " +
            "`converted` TEXT NOT NULL, " +
            "`convertedTargetId` INTEGER, " +
            "`archived` INTEGER NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`), " +
            "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, " +
            "FOREIGN KEY(`linkedSessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)",
        columns = listOf("id", "text", "detail", "kind", "isVoice", "projectId", "linkedSessionId", "converted", "convertedTargetId", "archived", "createdAt"),
    )
    execSQL("CREATE INDEX IF NOT EXISTS `index_captures_projectId` ON `captures` (`projectId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_captures_linkedSessionId` ON `captures` (`linkedSessionId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_captures_archived` ON `captures` (`archived`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_captures_kind` ON `captures` (`kind`)")
}

private fun SupportSQLiteDatabase.rebuildMemories() {
    rebuildTable(
        table = "memories",
        targetSql = "CREATE TABLE IF NOT EXISTS `memories` (" +
            "`id` INTEGER NOT NULL, " +
            "`title` TEXT NOT NULL, " +
            "`content` TEXT NOT NULL, " +
            "`type` TEXT NOT NULL, " +
            "`tags` TEXT NOT NULL, " +
            "`projectId` INTEGER, " +
            "`sourceSessionId` INTEGER, " +
            "`sourceCaptureId` INTEGER, " +
            "`sourceType` TEXT NOT NULL, " +
            "`revisitAt` INTEGER, " +
            "`archived` INTEGER NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`), " +
            "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, " +
            "FOREIGN KEY(`sourceSessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, " +
            "FOREIGN KEY(`sourceCaptureId`) REFERENCES `captures`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)",
        columns = listOf("id", "title", "content", "type", "tags", "projectId", "sourceSessionId", "sourceCaptureId", "sourceType", "revisitAt", "archived", "createdAt"),
    )
    execSQL("CREATE INDEX IF NOT EXISTS `index_memories_projectId` ON `memories` (`projectId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_memories_sourceSessionId` ON `memories` (`sourceSessionId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_memories_sourceCaptureId` ON `memories` (`sourceCaptureId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_memories_archived` ON `memories` (`archived`)")
}

private fun SupportSQLiteDatabase.rebuildTasks() {
    rebuildTable(
        table = "tasks",
        targetSql = "CREATE TABLE IF NOT EXISTS `tasks` (" +
            "`id` INTEGER NOT NULL, " +
            "`text` TEXT NOT NULL, " +
            "`tag` TEXT NOT NULL, " +
            "`projectId` INTEGER, " +
            "`captureId` INTEGER, " +
            "`dueAt` INTEGER, " +
            "`done` INTEGER NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`), " +
            "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, " +
            "FOREIGN KEY(`captureId`) REFERENCES `captures`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        columns = listOf("id", "text", "tag", "projectId", "captureId", "dueAt", "done", "createdAt"),
    )
    execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId` ON `tasks` (`projectId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_captureId` ON `tasks` (`captureId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_done` ON `tasks` (`done`)")
}
