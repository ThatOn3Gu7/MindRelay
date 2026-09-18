package com.mindrelay.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mindrelay.data.model.ProjectStatus

/** A project — the second-brain container for sessions, notes and memories. */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Long = 0,
    val name: String,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val currentState: String = "",
    val nextAction: String = "",
    val goal: String = "",
    val lastWorkedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
