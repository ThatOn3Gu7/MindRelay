# Changelog

Every user-facing or project-facing fix/feature gets an entry here, grouped by
category. GitHub release notes are generated from this file. Add one bullet per
change, newest first within a category.

## Unreleased

### Added

- Redesigned the Session screen (the last screen still on the old look) to
  match the app's expressive idiom: entrance animation, a live status chip,
  expressive entry cards with per-kind icon colors, a hero "current next
  action" card, and a pinned end-session action bar.

- Reworked the Settings Theme control from a detached dropdown into an
  expandable tile: tapping the Theme row grows the tile in place to reveal the
  three choices (System, Light, Dark) with `BrightnessAuto`/`LightMode`/
  `DarkMode` icons, the current setting stays highlighted, a rotating chevron
  and spring-based expand/collapse match the app's motion, and tapping outside
  collapses it.

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

### Changed

- Reworked the empty-state layout so Inbox, Projects, Memories, and Search share one fixed top-aligned artwork position; switching tabs now changes only the artwork and copy, not the empty-state placement.

- Removed the decorative Inbox and Search icons from their root tab top bars and let both titles use the same leading position as the other root-level screens, so switching tabs no longer makes the title appear to move.

- Removed the inert "Expressive style" tile from Settings → Appearance. It only
  showed "Rounded · Roboto Flex" with no action or behavior; the actual rounded
  expressive styling used across the app is untouched, and the Appearance
  section keeps its normal spacing with just the Theme control.

- Inbox and Search no longer show a back arrow in their top bars: both are
  root-level tabs, so their leading slot now shows a static, non-clickable
  contextual icon (`Inbox` / `Search`) via a new reusable `MindTopBar`
  `leadingIcon` slot. Back/close buttons on other screens are unchanged.

- Removed the inert tuning (`Tune`) icon from the Memories search field; the
  field itself and its searching/filtering behavior are unchanged.

### Fixed

- The CI build stopped at the lint step: the Memories and Projects tab lists
  never used the `AnimatedContent` target-state parameter, which
  `UnusedContentLambdaTargetStateParameter` reports as an error. Renaming it to
  `_` or dropping the declaration does not satisfy the check — the parameter has
  to be referenced — so both lists now key their content on the target filter,
  matching the Search screen. Filter chips, lists and crossfades are unchanged.
- CI now mirrors every lint finding from the full lint text report into
  check-run annotations, so a failing run shows all of them (AGP prints only
  the first failure to the console log).

- Three-dot (MoreVert) overflow menus now open beside their button instead of
  in a corner of the screen. The menus on Capture Detail, Memory Detail,
  Project Detail (including its "Change status" sub-menu) and Session were
  re-anchored: `MindTopBar` gained an `overflowMenu` slot and a shared
  `MindOverflowMenu` renders the MoreVert button and its menu(s) inside one
  anchored box, so the popup positions itself against the button across screen
  sizes. All menu items, behaviors, nested menus and outside-tap dismissal are
  unchanged.

- New/Edit Memory Type selector is now reliably tappable: `MindDropdown` was
  rewritten to the canonical Material 3 `ExposedDropdownMenuBox` + `menuAnchor`
  pattern (the whole field opens the menu, instead of a stacked `clickable` on
  the text field that could miss taps). Options are unchanged (Fix, Person,
  Idea, Place, Recipe, Note, Other), the selected option is highlighted, and
  editing preloads and persists the saved type via shared label/type mappings.

- The sample backup no longer shows mismatched "next action" rows after restore:
  each project's `nextAction` (and its active session's `currentNextAction`)
  now matches that project's youngest open task — e.g. "Compose Practise App"
  leads with "Draft the AGENTS.md project rules" instead of a ghost
  "Wire up the backup screen to the store" string that no task carried. A test
  now pins the invariant so a restored backup can never ship a next-action entry
  that opens a project showing a different action.

- Search results no longer open the wrong screen: tapping a task result used to
  bounce back to Home, so it now opens the task's owning project — or its source
  capture when it has no project — matching the Home "Next Actions" convention.
  A task with neither stays put instead of jumping to Home.

- Backup validation now rejects a memory whose "Related project" contradicts its
  "Source session"/"Source capture" (a memory cannot belong to one project while
  its source lives in another), and the shipped sample backup was repaired so
  its "Bed layout after winterizing" memory links the garden session that
  produced it instead of a bicycle session.

- Home "Next Actions" no longer disappear when tapped: tapping now navigates to
  the item's context (task-backed actions open their project, or source capture
  when unassigned; project-derived actions open their project) instead of
  silently marking the task complete and removing it.

- Fixed a compile break in the Data & Backup screen from an import that pointed
  `ConfirmDialog` at the wrong package.
- Fixed a lint error in Search where an `AnimatedContent` content lambda ignored
  its target filter state.
- Fixed compile errors from the latest screen redesigns: six screens imported
  `MindTopBar` from `ui.components` (it lives in `ui.screens`), and New Memory
  passed a `filled` argument that `MindDropdown` doesn't accept.

### Improved

- A read-write test in `BackupStoreTest` now parses and validates the shipped
  sample backup on every CI run, so the fixture can never silently drift from
  the real validation rules.
- CI no longer runs the workflow twice per pushed commit: the `pull_request`
  trigger is now limited to `opened`/`reopened` instead of also firing on
  `synchronize`.
