# MindRelay

Offline-first personal external memory for capturing fleeting thoughts, triaging
them later, preserving durable knowledge, and resuming projects with full
context for your Future Self.

Built for Android as a native Jetpack Compose app in the **Material 3
Expressive** design language (teal theme, Roboto Flex emphasized type, springy
expressive motion, pill/rounded component shapes), with light and dark mode
following the device's system setting.

## Highlights

- **Quick Capture** — zero-friction thought capture. Large field, idea/to-do/
  question/note chips and an optional project link. Saving never requires
  metadata. Capture is text-only: the mic icon is an affordance/illustration —
  MindRelay does **not** record or store audio (see "Voice capture" below).
- **Inbox** — a calm triage queue with filters (All / Ideas / To-dos / Questions
  / Voice). Each capture opens a **Capture Detail** sheet where it can be
  converted to a Memory, Project note, or Task, or archived — while preserving
  its conversion provenance.
- **Projects & Sessions** — active/paused/done projects; each project shows
  current state + one next action. Sessions are chronological records of note,
  discovery, question, decision and task entries ending in a handoff that
  completes atomically: it marks the session completed, updates the project's
  current state / next action / last-worked timestamp, and can promote
  discoveries to a Memory — all in one database transaction. Each session has a
  persisted display number ("Session N") assigned as max + 1 per project, so
  labels never collide with survivors after deletions. A project has at most
  one ACTIVE session at a time (mediated transactionally by the repository — see
  "Atomic writes" below).
- **Memories** — durable knowledge with tags, a type, a linked project/source
  session, and optional revisit dates. Only memories that are actually due
  resurface on Home; future revisits stay hidden until their date.
- **Search** — global search across projects, session entries, tasks, memories
  and captures, grouped by source type.
- **Settings & Data/Backup** — appearance, capture defaults, reminders, plus
  export/import/restore of everything in a `.json` file. No account, no cloud,
  no silent upload: everything lives in a local Room/SQLite database owned by you.

## Data is real

MindRelay persists everything on-device with Room (SQLite) + DataStore for
settings. New installs start with empty state and the app grows as you use it.
Backups preserve entity ids so every relationship round-trips correctly.

### Atomic writes

Multi-entity writes run inside single Room transactions, so a capture conversion
(capture → memory/task/project-note + provenance), a session handoff, and a
project deletion are all-or-nothing. Repeated taps are rejected idempotently:
converting an already-converted capture or completing an already-completed
session does nothing twice.

The "one ACTIVE session per project" invariant is mediated inside those same
transactions (check-then-create re-reads the active session under the Room
transaction, so a concurrent second caller observes the first caller's session
rather than creating a duplicate). A schema-level unique index — the composite
unique `sessions(projectId, displayNumber)` — guarantees session labels stay
unambiguous; Room cannot express a partial (`WHERE status = 'ACTIVE'`) unique
index in its entity schema, so the one-active rule is enforced by the
transactional repository rather than by the DB engine.

### Backup & restore

- **Export** writes all projects, sessions, entries, captures, memories and
  tasks to a `.json` file the user picks.
- **Import** fully parses and validates the file (identity, version, required
  fields, unique positive ids, enum values, timestamps, referential integrity
  and provenance consistency) before changing anything, then **merges**
  additively keyed by id: existing data is never overwritten unless the backup
  carries the same id.
- **Restore** shows a real preview of the entity counts, writes a local safety
  backup, then **replaces** the current data inside one transaction — no
  partial restore is possible.
- Settings (theme, capture defaults, reminders) are preferences and are
  **not** part of the JSON backup.
- A fully populated sample for restore/import testing ships at
  `docs/samples/mindrelay-sample-backup.json`, covering projects, sessions,
  entries, captures, memories and tasks with realistic cross-references. It is
  re-validated in CI by `BackupStoreTest.shippedSampleBackupParsesAndValidates`.

### Android system backup

The product promise is that your data lives only on your device. The manifest
sets `allowBackup=false` together with a `data-extraction-rules.xml` that
excludes all domains, so Android's cloud backup and device-to-device transfer do
**not** include MindRelay data or preferences. The only supported transfer path
is the in-app JSON export/import.

### Voice capture

Voice capture is **not implemented**. The mic icon is a design affordance only;
MindRelay records text and never captures, stores or uploads audio. The
"Voice capture" setting only controls whether the voice affordance appears and
does not enable any recording.

## Building

```bash
# Requires JDK 17 and the Android SDK (compileSdk 37).
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:lintDebug            # Android lint
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

Debug builds are signed with a **committed stable debug key**
(`keystore/mindrelay-debug.p12`, password/alias = `android`/`androiddebugkey`),
so every build — local or CI — shares one signature and a new CI build installs
straight over an older one instead of forcing an uninstall/reinstall. This is a
debug-only convenience key; it must never sign anything user-facing.

> The version matrix (material3 1.5.0-alpha22, Compose BOM 2026.08.00, AGP 9.2.1,
> Gradle 9.5.0, Kotlin 2.3.21, KSP 2.3.9, Room 2.8.4) mirrors the official
> Google `compose-samples` sample so the M3 Expressive APIs resolve correctly.

## CI

One canonical workflow (`.github/workflows/build-apk.yml`) runs on every push
and pull request: it runs the unit tests, runs Android lint, builds the debug
APK, and uploads the `mindrelay-debug-apk` artifact (plus build/lint/test
diagnostics on failure).

Two ways to get the built APK, with an important difference:

- **Workflow artifact** (`mindrelay-debug-apk`, on the run's *Artifacts* tab) —
  GitHub always wraps artifacts in a `.zip`, so you download a zip and unzip it
  before installing. This is a GitHub limitation, not a project bug.
- **Release asset** — every push to `main` publishes a **`mindrelay-latest`**
  release whose asset downloads as a raw, directly installable `.apk`
  (`mindrelay-debug-<sha>.apk`), since release assets are not zip-wrapped.
