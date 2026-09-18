package com.mindrelay.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A (possibly recurring) next-action task, auto-created for reviews. */
@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.SET_NULL,
    ), ForeignKey(
        entity = CaptureEntity::class,
        parentColumns = ["id"],
        childColumns = ["captureId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index("projectId"),
        Index("captureId"),
        Index("done"),
    ],
)
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
