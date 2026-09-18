package com.mindrelay.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mindrelay.data.model.SessionStatus

/**
 * A work period on a project — a chronological record ending in a handoff.
 * The "one ACTIVE session per project" invariant is mediated by the repository
 * inside a transaction (see MindRepository), and the composite unique index
 * `index_sessions_projectId_displayNumber` keeps "Session N" labels unambiguous.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index("projectId"),
        // At most one session per project may hold a given display number, so
        // "Session N" labels are unambiguous and never collide after deletes.
        Index(value = ["projectId", "displayNumber"], unique = true),
    ],
)
data class SessionEntity(
    @PrimaryKey val id: Long = 0,
    val projectId: Long,
    val title: String,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    /** Handoff fields, written atomically when the session ends. */
    val completed: String = "",
    val discoveries: String = "",
    val unresolved: String = "",
    val currentNextAction: String = "",
    /** Persisted per-project display number ("Session N"); survives deletions. */
    val displayNumber: Int = 0,
)
