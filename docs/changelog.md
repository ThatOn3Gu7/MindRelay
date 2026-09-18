# Changelog

Every user-facing or project-facing fix/feature gets an entry here, grouped by
category. GitHub release notes are generated from this file. Add one bullet per
change, newest first within a category.

## Unreleased

### Added

- Committed a stable debug signing key (`keystore/mindrelay-debug.p12`) so every
  debug APK — local or CI — shares one signature, and CI builds install straight
  over older builds instead of requiring an uninstall/reinstall.
- Published each merged `main` build as a **`mindrelay-latest`** GitHub release
  asset, so the debug APK downloads as a directly installable `.apk` instead of
  a workflow-artifact `.zip`.
- Added a fully populated sample backup
  (`docs/samples/mindrelay-sample-backup.json`) covering projects, sessions,
  entries, captures, memories and tasks with realistic cross-references, for
  hands-on restore/import testing.
- Added `docs/` changelog directory and a root `agents.md` with the project
  rules every agent/session must follow.

### Improved

- A read-write test in `BackupStoreTest` now parses and validates the shipped
  sample backup on every CI run, so the fixture can never silently drift from
  the real validation rules.
