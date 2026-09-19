package com.mindrelay.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindrelay.data.model.CaptureKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rootDataStore by preferencesDataStore(name = "mindrelay_settings")

/** Appearance / capture / reminder preferences (as distinct from mind data). */
class SettingsStore(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val defaultSave = stringPreferencesKey("default_save")
        val defaultCaptureKind = stringPreferencesKey("default_capture_kind")
        val voiceCapture = booleanPreferencesKey("voice_capture")
        val autoLink = booleanPreferencesKey("auto_link")
        val revisitReminders = booleanPreferencesKey("revisit_reminders")
        val lastBackupAt = longPreferencesKey("last_backup_at")
    }

    data class Settings(
        val theme: String = "System",           // System | Light | Dark
        val defaultSave: String = "Inbox",      // Inbox | Current project
        val defaultCaptureKind: String = "Note", // matches CaptureEntity default
        val voiceCapture: Boolean = true,
        val autoLink: Boolean = false,
        val revisitReminders: Boolean = true,
        val lastBackupAt: Long? = null,
    )

    val settings: Flow<Settings> = context.rootDataStore.data.map { p ->
        Settings(
            theme = p[Keys.theme] ?: "System",
            defaultSave = p[Keys.defaultSave] ?: "Inbox",
            defaultCaptureKind = p[Keys.defaultCaptureKind] ?: "Note",
            voiceCapture = p[Keys.voiceCapture] ?: true,
            autoLink = p[Keys.autoLink] ?: false,
            revisitReminders = p[Keys.revisitReminders] ?: true,
            lastBackupAt = p[Keys.lastBackupAt],
        )
    }

    suspend fun setTheme(value: String) = context.rootDataStore.edit { it[Keys.theme] = value }
    suspend fun setDefaultSave(value: String) = context.rootDataStore.edit { it[Keys.defaultSave] = value }
    suspend fun setDefaultCaptureKind(value: String) = context.rootDataStore.edit { it[Keys.defaultCaptureKind] = value }
    suspend fun setVoiceCapture(value: Boolean) = context.rootDataStore.edit { it[Keys.voiceCapture] = value }
    suspend fun setAutoLink(value: Boolean) = context.rootDataStore.edit { it[Keys.autoLink] = value }
    suspend fun setRevisitReminders(value: Boolean) = context.rootDataStore.edit { it[Keys.revisitReminders] = value }
    suspend fun setLastBackupAt(value: Long) = context.rootDataStore.edit { it[Keys.lastBackupAt] = value }
}

/** Map a stored capture-kind string to the enum (never throws). */
fun String.toCaptureKindOrNull(): CaptureKind? = CaptureKind.entries.firstOrNull { it.name == this }
