## 1. Connector Architecture

- [x] 1.1 Define `SubmitConnector` interface with `id`, `submitItems()`, `submitSummary()` — verified by existing interface in `submit/SubmitConnector.kt`
- [x] 1.2 Create `SubmitConfig` and `ConnectorConfig` data classes — verified by existing config model in `submit/SubmitConfig.kt`
- [x] 1.3 Wire `SubmitConfig` into `ConfigRoot` and `ConfigModule` — verified by existing `ConfigRoot.submit` field and `ConfigModule.provideSubmitConfig()`
- [x] 1.4 Create `SubmitModule` with Dagger `@IntoSet` bindings — verified by existing `submit/SubmitModule.kt`

## 2. Submit Status Tracking

- [x] 2.1 Implement `SubmitStatusTracker` with per-item per-connector status — verified by `SubmitStatusTrackerTest` (14 tests)
- [x] 2.2 Implement content-based item keying (`Base64(activity|start|end)`) — verified by `shouldRecognizeSameItemByContent` test
- [x] 2.3 Implement persistence to `~/.stt/submit-status` with `start()`/`stop()` lifecycle — verified by `shouldPersistAndReloadOnStart` and `shouldCreateStatusDirectoryOnStart` tests
- [x] 2.4 Implement `isSubmitted()`, `markSubmitted()`, `getSubmitFraction()`, `requireNotSubmitted()` methods — verified by existing tests

## 3. Selection Management

- [x] 3.1 Implement `SubmitSelectionManager` with per-connector selection sets — verified by `SubmitSelectionManagerTest` (10 tests)
- [x] 3.2 Ensure same content-based key scheme as `SubmitStatusTracker` — verified by identical `itemKey()` implementation

## 4. JSON Connector

- [x] 4.1 Implement `JsonSubmitConnector` writing individual items as JSON array — verified by `JsonSubmitConnectorTest` (11 tests)
- [x] 4.2 Implement `submitSummary()` for `ReportListItem` serialization — verified by `shouldWriteCorrectJsonForSummary` test
- [x] 4.3 Support absolute and relative output paths — verified by `shouldUseAbsolutePathWhenFileStartsWithSlash` test
- [x] 4.4 Implement file overwrite (truncate) on each submit — verified by `shouldOverwriteExistingFileOnEachSubmit` test

## 5. Item Locking

- [x] 5.1 Inject `SubmitStatusTracker` into `Activities` command handler — verified by constructor parameter in `command/Activities.kt`
- [x] 5.2 Add `requireNotSubmitted()` guard to all mutation methods — verified by `IllegalStateException` for submitted items
- [x] 5.3 Disable action buttons in `TimeTrackingItemCellWithActions` for submitted items — verified by UI button disable logic

## 6. Activities View UI

- [x] 6.1 Add connector ComboBox to Activities view toolbar — verified by `addSubmitToolbar()` in `ActivitiesController.kt`
- [x] 6.2 Add per-cell submit checkbox to `TimeTrackingItemCellWithActions` — verified by cell checkbox setup
- [x] 6.3 Implement "Submit Selected" button — verified by `submitSelectedItems()` method

## 7. Report View UI

- [x] 7.1 Add connector ComboBox to Report view toolbar — verified by `addSubmitToolbar()` in `ReportController.kt`
- [x] 7.2 Add three-state checkbox column (all/some/none submitted) — verified by `addSubmitCheckboxColumn()` in `ReportController.kt`
- [x] 7.3 Implement "Submit Selected" button with partial submit — verified by `submitSelectedRows()` method

## 8. Cross-View Sync

- [x] 8.1 Define `ItemsSubmitted` event class — verified by existing `event/Event.kt`
- [x] 8.2 Publish event after successful submit in both views — verified by event bus calls in both controllers
- [x] 8.3 Subscribe both controllers to refresh on `ItemsSubmitted` — verified by `onItemsSubmitted` handlers

## 9. Dagger Wiring

- [x] 9.1 Include `SubmitModule` in `UIApplication` Dagger component — verified by `UIApplication.kt`
- [x] 9.2 Include `SubmitModule` in `CLIApplication` Dagger component — verified by `CLIApplication.kt`
- [x] 9.3 Start `SubmitStatusTracker` as lifecycle service in `UIMain` — verified by `startService()` call in `UIMain.kt`