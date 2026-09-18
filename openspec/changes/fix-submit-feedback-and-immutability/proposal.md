# Proposal: Fix Submit Feedback and Immutability Indication

## Why

Submitting a group from the Report view succeeds (Tempo receives the worklog, `submit-status` is written, log says "Created Tempo worklog") but the UI gives almost no feedback: no popup appears, the Report view shows no immutability state, and the Activities view only shows the locked state after an app restart. The user cannot tell whether the submit worked without leaving the app. Additionally, submitting from the Activities view can never find checked items because of a connector-ID mismatch.

## What Changes

- **Submit result notifications with owner window**: submit results (success summary and failures) are shown via a notification that is anchored to an owner window so it reliably appears on all platforms (ControlsFX `Notifications` with explicit owner, falling back to the Report view's existing `NotificationPane` when no window is available).
- **Visual immutability via lock icon**: report rows whose backing items are all submitted to any connector show a dedicated lock icon (instead of the initially planned strikethrough); the same lock icon is shown in the Activities view for submitted items.
- **Synchronized connector selection**: the connector ComboBoxes in the Report and Activities views stay in sync — selecting a connector in one view immediately selects it in the other.
- **Persisted last-used connector with immediate save**: the connector chosen in the connector ComboBox is persisted to the configuration immediately on change (not only on shutdown); both views default to the last-used connector on startup instead of the first connector in the list.
- **Persisted last-used connector**: the connector chosen in the connector ComboBox is persisted in the config; both views (Report and Activities) default to the last-used connector on startup instead of the first connector in the list.
- **Fix connector-ID mismatch**: `ActivitiesController.submitSelectedItems()` uses the connector `id` (e.g. `tempo`) instead of `javaClass.simpleName.lowercase()` (`temposubmitconnector`) when reading selections, so checked items are actually found.

## Capabilities

### New Capabilities
- `submit/connector-selection`: Persisted last-used connector selection shared by the Activities and Report views (defaulting, persistence, fallback behavior).

### Modified Capabilities
- `submit/ui-report-view`: Add a requirement for a row-level submitted/immutability indication via a lock icon that updates on `ItemsSubmitted`; add requirement that the connector ComboBox is preselected with the last-used connector.
- `submit/ui-activities-view`: Add requirement for a lock icon on submitted items; add requirement that selection lookup during submit uses the same connector ID that checkboxes store (`connector.id`); add requirement that the connector ComboBox is preselected with the last-used connector.
- `submit/cross-view-sync`: Extend so that submit results are surfaced to the user via a reliable, owner-anchored notification (success summary and failures), covering the current silent-success path; add requirement that connector selection changes are synchronized across views.
- `submit/connector-selection`: Add requirement that the last-used connector is persisted immediately when the selection changes and that both views' selections stay synchronized.

## Impact

- **Code**: `ReportController` (checkbox column, row styling, toolbar), `ActivitiesController` (submit selection lookup, toolbar), `TimeTrackingItemCellWithActions` (no functional change expected), `MainWindowController` (notification handling), `TempoSubmitConnector` (notification routing unchanged, owner handled centrally), `SubmitSelectionManager` (no change), config package (new persisted setting), `ConfigModule`/`SubmitModule` wiring.
- **Config**: New persisted setting for the last-used connector id under the existing YAML config (survives restarts, defaults to first connector when unset or stale).
- **Tests**: `ActivitiesControllerTest`, `ReportBindingTest`, new tests for connector persistence and submit notifications.
- **No breaking changes**: config file remains backward compatible (missing key falls back to first connector).
