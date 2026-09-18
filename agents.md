# agents.md — Project rules for MindRelay

Standing rules for **any** agent or future session working in this repository.
Read this first; follow it unless the user explicitly overrides a rule.

## What this project is

MindRelay is an offline-first personal external-memory app (Android, Jetpack
Compose, Material 3 Expressive). Core promise: **Capture → Organize → Remember →
Resume**, with trustworthy long-term personal context. Everything is stored
locally (Room/SQLite + DataStore) — no account, no cloud, no silent upload.

It is a small, personal app. Keep it understandable; prefer focused fixes over
rewrites. Do **not** add speculative product features and do **not** turn it
into generic project management.

## Repo layout

- `app/src/main/java/com/mindrelay/…` — app code (`data/` = storage layer,
  `ui/` = Compose screens, `nav/` = navigation).
- `app/src/test/java/com/mindrelay/…` and `app/src/test/resources/…` — JVM unit
  tests and their fixtures (the sample backup lives under
  `app/src/test/resources/backup/`).
- `docs/` — changelog (`changelog.md`) and samples. GitHub release notes are
  generated from `docs/changelog.md`.
- `.github/workflows/build-apk.yml` — the one canonical CI workflow: unit tests,
  lint, debug APK build, `mindrelay-debug-apk` artifact, diagnostics.
- `keystore/mindrelay-debug.p12` — committed stable **debug** signing key
  (password/alias are the standard `android` / `androiddebugkey`; debug-only,
  never reuse it for anything real).

## Standing rules

1. **Preserve visual design.** Do not redesign screens unless asked. The Home
   screen redesign is the user's own WIP — never overwrite or revert it.
2. **Preserve data.** Never silently erase user data. Never "fix" a schema
   change by deleting the database — add deliberate manual Room migrations.
3. **Centralize invariants.** Domain rules live in the repository/data layer,
   not duplicated across Compose screens.
4. **Do not implement fake voice capture.** Voice capture is not a feature; the
   mic is an affordance only. Never add audio recording/storage.
5. **Do not blindly upgrade dependencies or SDK versions.** The version matrix
   (material3 1.5.0-alpha22, Compose BOM 2026.08.00, AGP 9.2.1, Gradle 9.5.0,
   Kotlin 2.3.21, KSP 2.3.9, Room 2.8.4) mirrors the official `compose-samples`.
6. **Do not reintroduce `org.jetbrains.kotlin.android`.** This project uses AGP
   9+ built-in Kotlin support + `org.jetbrains.kotlin.plugin.compose`.
7. **Preserve IDs and relationships** wherever possible. Backups round-trip
   entity ids so relationships survive.
8. **Backups validate fully before any mutation, then mutate transactionally.**
   The JSON envelope is `{ app: "MindRelay", version: 1, projects, sessions,
   entries, captures, memories, tasks }`. Import = additive merge keyed by id;
   restore = full same-id replacement after a local safety backup. Settings
   (DataStore) are intentionally **not** in the JSON backup. Android system
   backup is disabled (`allowBackup=false` + data-extraction-rules).
9. **Write reliability.** Navigate only after writes succeed; surface launch
   errors via the existing `SharedFlow<String?>` error channel.

## Commit message style

Match the user's latest commit pattern exactly (no trailers, no co-authors):

```
feat(home): redesign Home screen with interactive expressive cards

- bullet summary of the change
- optional second bullet

```

Short "audit / CI-fix" follow-up commits used a bare subject line without a
body — prefer the full form with a bullet body for substantive work. Vary the
scope (`feat(…):`, subject) to fit the change.

## Release cadence

- Create a new GitHub release every **20–25 commits** that carry impactful
  changes to the app.
- Every fix/feature also gets a one-line entry in `docs/changelog.md`, grouped
  under **Added / Changed / Improved / Fixed**, newest first. GitHub release
  notes reuse this file.

## Build & CI loop

No local JDK/Kotlin/Gradle is available in the agent environment — GitHub
Actions is the primary compile/test/build signal. The push-triggered workflow
runs on every push and PR:

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:lintDebug           # Android lint
./gradlew :app:assembleDebug       # debug APK → app/build/outputs/apk/debug/app-debug.apk
```

- Push early and watch the workflow run; read job logs via
  `gh run list` / `gh run view` and fix iteratively. Do not stop at the first
  error.
- GitHub wraps workflow artifacts in a `.zip` (that is not a bug you can fix in
  the upload step). The direct-install path is the `mindrelay-latest` release
  asset published from `main` pushes.

## Working-branch rules (this session)

- Work only on `arena/01a0b317-mindrelay`, push only to it, and open/update the
  PR from it. Never create or push other branches.
- Only claim a push/PR/release happened if it actually succeeded.
- Validate with the tools that actually run; report results truthfully,
  including what could not be verified.
