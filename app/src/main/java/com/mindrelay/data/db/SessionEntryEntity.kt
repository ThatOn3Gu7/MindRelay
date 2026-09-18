package com.mindrelay.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mindrelay.data.model.EntryKind

/** One chronological entry inside a session: note, discovery, question, decision, task. */
@Entity(tableName = "session_entries")
data class SessionEntryEntity(
    @PrimaryKey val id: Long = 0,
    val sessionId: Long,
    val text: String,
    val kind: EntryKind,
    val createdAt: Long = System.currentTimeMillis(),
)
