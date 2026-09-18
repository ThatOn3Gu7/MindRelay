# MindRelay

Offline-first personal external memory for capturing fleeting thoughts, triaging
them later, preserving durable knowledge, and resuming projects with full
context for your Future Self.

Built for Android as a native Jetpack Compose app in the **Material 3
Expressive** design language (teal theme, Roboto Flex emphasized type, springy
expressive motion, pill/rounded component shapes), with light and dark mode
following the device's system setting.

## Highlights

- **Quick Capture** — zero-friction thought capture. Large field, mic affordance,
  optional idea/to-do/question chip and project link. Saving never requires metadata.
- **Inbox** — a calm triage queue with filters (All / Ideas / To-dos / Questions /
  Voice). Each capture opens a **Capture Detail** sheet where it can be converted
  to a Memory, Project note, Task, or archived while preserving provenance.
- **Projects & Sessions** — active/paused/done projects; each project shows
  current state + one next action. Sessions are chronological records of note,
  discovery, question, decision and task entries ending in a handoff that
  atomically updates the project and can promote discoveries to Memories.
- **Memories** — durable knowledge with tags, a type, a linked project/source
  session, and optional revisit dates (revisit dates resurface on Home).
- **Search** — global search across projects, session entries, memories and
  captures, grouped by source type.
- **Settings & Data/Backup** — appearance, capture defaults, reminders, plus
  export/import/restore of everything in a `.json` file. No account, no cloud,
  no silent upload: everything lives in a local Room/SQLite database owned by you.

## Data is real

MindRelay persists everything on-device with Room (SQLite) + DataStore for
settings. There is no sample/demo data — new installs start with empty state and
the app grows as you use it. Backups preserve entity ids so every relationship
round-trips correctly.

## Build

```bash
# Requires JDK 17 and the Android SDK (compileSdk 37).
./gradlew :app:assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
A GitHub Actions workflow (`.github/workflows/build-apk.yml`) builds and uploads
the APK as an artifact on every push — that artifact is the deliverable.

> The version matrix (material3 1.5.0-alpha22, Compose BOM 2026.08.00, AGP 9.2.1,
> Gradle 9.5.0, Kotlin 2.3.21, KSP 2.3.9, Room 2.8.4) mirrors the official
> Google `compose-samples` sample so the M3 Expressive APIs resolve correctly.
