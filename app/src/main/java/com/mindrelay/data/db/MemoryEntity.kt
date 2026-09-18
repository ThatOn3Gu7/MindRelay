package com.mindrelay.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mindrelay.data.model.MemorySourceType
import com.mindrelay.data.model.MemoryType

/** Durable personal knowledge — findable by title, content and tags. */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: Long = 0,
    val title: String,
    val content: String = "",
    val type: MemoryType = MemoryType.NOTE,
    val tags: String = "",
    val projectId: Long? = null,
    val sourceSessionId: Long? = null,
    val sourceCaptureId: Long? = null,
    val sourceType: MemorySourceType = MemorySourceType.MANUAL,
    val revisitAt: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
