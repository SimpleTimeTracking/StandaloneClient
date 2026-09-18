# Design: Fix Submit Feedback and Immutability Indication

## Context

The submit pipeline itself works: `TempoSubmitConnector.submitSummary()` creates worklogs, `SubmitStatusTracker.markSubmitted()` persists status synchronously (`~/.stt/submit-status`), and `ItemsSubmitted` is published. The failure is entirely in the feedback layer:

- `MainWindowController.onUserNotifactionRequest` shows ControlsFX `Notifications.create().text(...).show()` **without an owner window** — on macOS these ownerless toasts are unreliable and silently never render, so the success summary ("Tempo: N submitted...") disappears.
- The Report view's only submitted-state indicator is the 40px checkbox column, computed per-connector against the **active connector**. Both views default their ComboBox to `selectionModel.selectFirst()`, which is the *json* connector (a default-config connector), not tempo — so rows submitted to tempo look unsubmitted after restart.
- Report rows have no other visual submitted state; `ReportListItem`/`ReportingItem` carry no submit info and are only rebuilt on `ItemModified`/date change.
- `ActivitiesController.submitSelectedItems()` reads selections under `connector.javaClass.simpleName.lowercase()` while checkboxes store under `connector.id` — activities-view submits can never find checked items.

Constraints: JavaFX 21 UI on the FX Application Thread; MBassador synchronous dispatch; config persisted through `ConfigServiceFacade` (JSON via `stt.json` is primary, YAML legacy); connectors are a `Set<SubmitConnector>` with string `id`s.

## Goals / Non-Goals

**Goals:**
- Reliable submit-result feedback anchored to the main window, from both views.
- Row-level visual immutability in the Report view, updated in-session and after restart.
- Last-used connector persisted and preselected in both views.
- Correct selection lookup in the Activities view.

**Non-Goals:**
- Changing the submit pipeline, `SubmitStatusTracker` persistence format, or connector implementations.
- Per-row partial-submit styling beyond the existing checkbox (indeterminate state stays as is).
- Reworking `ReportingItem` to carry submit state (computed at render time instead — see Decision 3).

## Decisions

### 1. Notification owner: store the primary Stage in `MainWindowController`

`MainWindowController.show(stage: Stage)` receives the real primary stage; store it in a field and change `onUserNotifactionRequest` to `Notifications.create().owner(stage).text(event.message).show()`. Guards: if the stage is not yet showing, fall back to ownerless show (never throw from the handler).

*Alternatives considered:* injecting the `stageProvider()` from `JFXModule` — rejected, it returns a new empty `Stage`, not the primary one. Using the Report view's `NotificationPane` for submit results — rejected, it lives inside one tab and is invisible from the Activities tab; the toast must be window-level.

### 2. Last-used connector persisted in `SubmitConfig`, saved immediately on change

Add `lastUsedConnectorId: String? = null` to `SubmitConfig` (and to the JSON/YAML serialization — it is a plain nullable string field on an existing container). Both `ActivitiesController.addSubmitToolbar()` and `ReportController.addSubmitToolbar()`:

- On build: select the connector whose `id` matches `configService.config.submit.lastUsedConnectorId`; if null/unknown, `selectFirst()`.
- On combo change: write the id back to `ConfigRoot.submit.lastUsedConnectorId` and call a new `ConfigServiceFacade.save()` (delegating to the underlying `JsonConfigService`/`YamlConfigService` save) **immediately**. Rationale: relying on shutdown-time persistence alone was observed to lose the selection in practice (`stt.json` was never rewritten on the user's machine), so every selection change is flushed to disk right away.

*Alternatives considered:* a separate properties file — rejected, existing config infra already handles read/write and DI; an in-memory-only "last used" shared service — rejected, user explicitly wants persistence across restarts; shutdown-only persistence — rejected, observed to be unreliable.

### 3. Immutability indicator: lock icon computed at render time

Do not add submit state to `ReportListItem`/`ReportingItem`/`SummingReportGenerator`. Instead, a Font Awesome `LOCK` glyph is added to the `Glyph` enum and rendered by both views when all of a group's items are submitted to any connector:

- Report view (`ActivityTableCell` for grouped rows, plain comment cells otherwise): the lock icon is appended to the cell graphic.
- Activities view (`TimeTrackingItemCellWithActions`): the lock icon is inserted into the cell pane next to the item when `isSubmitted(item)` (any connector).

Both re-read the tracker inside their `updateItem`/refresh paths, so `tableForReport.refresh()` (already called from `submitSelectedRows()` and `onItemsSubmitted`) and `activityList.refresh()` are sufficient to update visuals in-session — no model invalidation needed, and `ItemModified` handling stays untouched. This replaces the originally planned strikethrough, which was too subtle.

*Alternatives considered:* invalidating `reportModel` on `ItemsSubmitted` — rejected: rebuilds the whole report from the queries cache and would drop any transient state for no benefit, since the data (durations/comments) does not change on submit.

### 3a. Connector selection synchronized across views via event bus

A new `ConnectorSelected` event is published on the existing MBassador bus whenever a combo selection changes. Both controllers subscribe and, when the event's connector id differs from their own selection, select the local connector with that id. Matching is done **by id, not by reference**: connector instances were previously duplicated per view (unscoped providers), and reference-based selection would clear the combo to empty (observed bug). The connector providers in `SubmitModule` are now `@Singleton`, so both views share the same instances (single caches/counters per connector); the id-based matching is kept as a robust safeguard. Selecting the same value fires no change event, so propagation terminates naturally (no oscillation). Startup preselection from the config already makes both views consistent initially; `lateinit` guards protect handlers from firing before a view's toolbar is built.

### 4. Activities-view selection lookup fix

In `ActivitiesController.submitSelectedItems()`, replace `connector.javaClass.simpleName.lowercase()` with `connector.id`. This aligns with `TimeTrackingItemCellWithActions` (which stores under `connectorIdProvider()` = `activeConnector?.id`) and `ReportController`. No migration needed: selections are in-memory only.

### 5. Feedback path stays event-based

`TempoSubmitConnector` (and the CSV/JSON connectors) keep publishing `NotifyUser` via the event bus; only the rendering side (`MainWindowController`) gains an owner. This keeps CLI mode unaffected (`Optional<MBassador>` empty → no-op) and matches the existing `NotifyUser` contract.

## Risks / Trade-offs

- [ControlsFX `Notifications.owner()` still misbehaves on some macOS setups] → Guard with try/catch and log; the lock icon and checkbox state provide secondary visual confirmation regardless of the toast.
- [Config save on every combo change writes the file mid-session] → Only the id string changes; the save is small and atomic enough for a single key, and it is required for the selection to survive abnormal termination.
- [Lock icon may be subtle for long lists] → Checkbox disabled state remains the connector-specific indicator; the lock icon is the any-connector immutable indicator, consistent across both views.
- [`TableView.refresh()` re-runs `updateItem` only for visible cells] → Acceptable: off-screen cells re-render on scroll; verified behavior matches how the checkbox column already refreshes.
- [`ConnectorSelected` event ping-pong between views] → Selecting an already-selected value fires no change event, so the propagation terminates after one hop.

## Migration Plan

No data migration. New config key is optional (`null` default); missing key → fallback to first connector. Rollback = revert commit; stale `lastUsedConnectorId` in an older build is simply ignored.

## Open Questions

None.
