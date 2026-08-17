# SimpleTimeTracking — UI Architecture

## Overview

Single-window, **tab-based JavaFX interface** with four tabs. All panels live inside a `TabPane`; there are no separate windows.

```
UIMain (JavaFX Application entry point)
  └── DaggerUIApplication (DI component)
        └── MainWindowController (orchestrator)
              ├── ActivitiesController
              ├── ReportController
              ├── SettingsController
              └── InfoController
```

---

## 1. Activities Tab — `ActivitiesController.kt` + `ActivitiesPanel.fxml`

Primary workspace: enter new time entries, view/search history, edit/delete/continue/stop items.

| UI Element | Purpose | Data Object |
|---|---|---|
| **Command text area** (`StyleClassedTextArea` via RichTextFX) | Type activity text (ANTLR grammar); `Ctrl+Enter` executes, `Ctrl+Space` auto-completes | Parsed into `Command` sealed hierarchy (`StartItem`, `StopItem`, `DeleteItem`, `EditItem`, ...) |
| **Activity list** (`ListView<TimeTrackingItem>`) | Scrollable list of all items, grouped by day with date headers | `ObservableArrayList<TimeTrackingItem>` — full in-memory set |
| **Per-item action buttons** (continue, stop, edit, delete) | Appear on hover via `FadeTransition` in `TimeTrackingItemCellWithActions` | Operates on a single `TimeTrackingItem` |
| **Search/filter field** | Real-time list filtering | Filters `allItems` by activity text match via `TimeTrackingListFilter` |
| **Worktime pane** (`WorktimePane`, embedded `FlowPane`) | Shows remaining worktime today (or overtime) and weekly total | Queries `WorkTimeQueries`; updates every 1s via `TimePassedEvent` |

**Data flow**: User types → `CommandFormatter` (ANTLR) parses → `ValidatingCommandHandler` validates → `CommandHandler` persists to `.stt/activities` → `ItemModified` events fire on the bus → all listeners refresh.

**Dialogs triggered from this tab**:
- Delete-confirm: `STTOptionDialogs.showDeleteOrKeepDialog()`
- Overlap warning: `STTOptionDialogs.showItemCoversOtherItemsDialog()`
- Bulk-rename prompt: `STTOptionDialogs.showRenameDialog()`
- No-current-item: `STTOptionDialogs.showNoCurrentItemAndItemIsLaterDialog()`

---

## 2. Report Tab — `ReportController.kt` + `ReportPanel.fxml`

Daily report with date picker, grouped activity table, and summary sidebar.

| UI Element | Purpose | Data Object |
|---|---|---|
| **DatePicker** (custom day-cell rendering) | Select a date; tracked days are highlighted | Returns `LocalDate` |
| **Report table** (`TableView<ReportListItem>`) | Grouped activities: comment, raw duration, rounded duration | `ReportListItem` (comment, isBreak, duration, roundedDuration) |
| **Summary labels** | Sidebar: total, effective, break, uncovered, non-effective, start/end | Computed from `SummingReportGenerator.Report` (`List<ReportingItem>` + duration totals) |

**Data flow**: Date selected → `ReportBinding` (an `ObjectBinding`) computes a `Report` via `SummingReportGenerator` using a `Criteria` query → `MappedListBinding` transforms into `ReportListItem` rows.

---

## 3. Settings Tab — `SettingsController.kt` (no FXML, programmatic UI)

ControlsFX `PropertySheet` populated from config beans.

| UI Element | Purpose | Data Object |
|---|---|---|
| **ControlsFX `PropertySheet`** | Auto-generated editable property grid | `ConfigRoot`, `ActivitiesConfig`, `BackupConfig`, `WorktimeConfig`, `JiraConfig`, `CommonPrefixGrouperConfig`, `ReportConfig`, `CliConfig` |
| **Custom editors** | `PathSetting` (file chooser button), `PasswordSetting` (obfuscated field) | Wrapper types for special config values |

Persistence happens on shutdown via `ConfigServiceFacade`.

---

## 4. Info Tab — `InfoController.kt` + `InfoPanel.fxml`

App metadata and update check.

| UI Element | Purpose | Data Object |
|---|---|---|
| **Version / Commit labels** | Display app metadata | `@Named("version")`, `@Named("commit hash")` strings |
| **"Check for updates" button** | Triggers async `UpdateChecker` | `UpdateChecker` queries a remote URL |
| **Project homepage link** | Opens browser | Hardcoded URL |

---

## Event Bus Wiring

All controllers communicate through a shared `MBassador<Any>` singleton (Dagger-provided).

| Publisher | Event | Listeners |
|---|---|---|
| `ActivitiesController` | `ItemInserted`, `ItemDeleted`, `ItemReplaced` | `ActivitiesController` (self-refresh), `ReportController.OnItemChangeListener`, `WorktimePane` |
| `UIMain` (1s timer) | `TimePassedEvent` | `WorktimePane` (recalc worktime) |
| `MainWindowController` (ESC/close) | `ShuttingDown` | `UIMain` (service stop + `Platform.exit()`) |
| Any controller | `NotifyUser` | `MainWindowController` (show ControlsFX `Notification`) |
| `BulkRenameHelper` (inside ActivitiesController) | `ItemReplaced` (monitors single edits) | Detects single-item rename and prompts to rename all matching items |

---

## Model Classes (Core)

| Class | Key Fields | Role |
|---|---|---|
| `TimeTrackingItem` | `activity: String`, `start: LocalDateTime`, `end: LocalDateTime?` | Fundamental persistence unit — one line in `.stt/activities` |
| `ReportingItem` | `duration`, `roundedDuration`, `comment`, `isBreak` | Aggregate view for report rows |
| `SummingReportGenerator.Report` | `reportingItems`, `start`, `end`, `uncoveredDuration`, etc. | Complete daily report payload |
| `Command` (sealed) | Subtypes: `StartItem`, `StopItem`, `DeleteItem`, `EditItem`, ... | Parsed user input from command text area |
| `Criteria` | `start`, `end`, `activity` | Query specification for filtering items |
| `ItemModified` / `ItemInserted` / `ItemDeleted` / `ItemReplaced` | Respective payload fields | Event bus messages |

## Config Objects (mapped to Settings tab)

| Class | Purpose |
|---|---|
| `ActivitiesConfig` | UI behavior: grouping, filtering duplicates, close-on-continue/stop, ask-before-delete, delete-closes-gaps |
| `BackupConfig` | Backup file settings |
| `WorktimeConfig` | Worktime rules for overtime/remaining-time calculation |
| `JiraConfig` | Jira REST API credentials and settings |
| `CommonPrefixGrouperConfig` | Text grouping settings |
| `ReportConfig` | Report formatting options |
| `CliConfig` | CLI encoding settings |
