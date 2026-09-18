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

/** What a capture has been converted into, if anything. */
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
