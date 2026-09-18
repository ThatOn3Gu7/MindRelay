package com.mindrelay.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A work period on a project — a chronological record ending in a handoff. */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: Long = 0,
    val projectId: Long,
    val title: String,
    val status: String = "Active", // Active | Completed
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    /** Handoff fields, written atomically when the session ends. */
    val completed: String = "",
    val discoveries: String = "",
    val unresolved: String = "",
    val currentNextAction: String = "",
)
