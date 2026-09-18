package com.mindrelay.data.graph

import android.content.Context
import androidx.room.Room
import com.mindrelay.data.backup.BackupStore
import com.mindrelay.data.db.AppDatabase
import com.mindrelay.data.repo.MindRepository
import com.mindrelay.data.settings.SettingsStore

/**
 * Manual, single-instance dependency graph (no DI framework): one Room database,
 * one settings DataStore, one repository shared across the whole app/process.
 */
class AppGraph(
    val database: AppDatabase,
    val repository: MindRepository,
    val settings: SettingsStore,
    val backup: BackupStore,
) {
    companion object {
        fun create(context: Context): AppGraph {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "mindrelay.db",
            ).addMigrations(*AppDatabase.MIGRATIONS).build()
            val settings = SettingsStore(context.applicationContext)
            return AppGraph(
                database = db,
                repository = MindRepository(db, settings),
                settings = settings,
                backup = BackupStore(db),
            )
        }
    }
}
