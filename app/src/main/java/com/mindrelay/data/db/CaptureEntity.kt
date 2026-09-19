package com.mindrelay.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.model.ConvertType

/**
 * A captured fleeting thought. Kinds: idea, to-do, question, note, voice.
 * `converted` + `convertedTargetId` record provenance once the thought is
 * promoted. `archived` items leave the active inbox but stay forever-logged
 * locally, keeping their conversion provenance intact.
 */
@Entity(
    tableName = "captures",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.SET_NULL,
    ), ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["linkedSessionId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [
        Index("projectId"),
        Index("linkedSessionId"),
        Index("archived"),
        Index("kind"),
    ],
)
data class CaptureEntity(
    @PrimaryKey val id: Long = 0,
    val text: String,
    val detail: String = "",
    val kind: CaptureKind = CaptureKind.NOTE,
    val isVoice: Boolean = false,
    val projectId: Long? = null,
    val linkedSessionId: Long? = null,
    val converted: ConvertType = ConvertType.NONE,
    val convertedTargetId: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
