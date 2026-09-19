package com.mindrelay.data.model

/** The type of a capture — the filter chips on Inbox map to these. */
enum class CaptureKind { IDEA, TODO, QUESTION, NOTE, VOICE }

/** The type / category of a durable memory. */
enum class MemoryType { FIX, PERSON, IDEA, PLACE, RECIPE, NOTE, OTHER }

/** A project's lifecycle state. */
enum class ProjectStatus { ACTIVE, PAUSED, DONE }

/** One chronological entry inside a work session. */
enum class EntryKind { NOTE, DISCOVERY, QUESTION, DECISION, TASK }

/** Where a memory came from (provenance), shown as chips on Memory Detail. */
enum class MemorySourceType { CAPTURE, SESSION, MANUAL, NONE }

/** The lifecycle state of a work session. Stored as the enum name (see Converters). */
enum class SessionStatus { ACTIVE, COMPLETED }

/**
 * What a capture has been converted into, if anything. `ARCHIVED` is kept only
 * for backward compatibility with existing backups; new data never uses it —
 * archiving is recorded via [com.mindrelay.data.db.CaptureEntity.archived] so
 * the conversion provenance always survives archiving.
 */
enum class ConvertType {
    NONE, MEMORY, PROJECT_NOTE, TASK, ARCHIVED;

    val label: String
        get() = when (this) {
            NONE -> "None"
            MEMORY -> "Memory"
            PROJECT_NOTE -> "Project note"
            TASK -> "Task"
            ARCHIVED -> "Archived"
        }
}
