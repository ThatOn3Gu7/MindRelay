package com.mindrelay.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A (possibly recurring) next-action task, auto-created for reviews. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Long = 0,
    val text: String,
    val tag: String = "",
    val projectId: Long? = null,
    val captureId: Long? = null,
    val dueAt: Long? = null,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
