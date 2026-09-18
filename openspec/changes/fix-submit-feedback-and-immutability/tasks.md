## 1. Last-Used Connector Persistence

- [x] 1.1 Add `lastUsedConnectorId: String? = null` to `SubmitConfig`; verify JSON/YAML config round-trips the new field (`JsonConfigService`/`YamlConfigService` save/load test or manual run)
- [x] 1.2 In `ReportController.addSubmitToolbar()`: preselect connector by stored id (fallback `selectFirst()`), and persist the id via `ConfigServiceFacade` when the combo selection changes; verify by switching connectors, restarting, and checking the same connector is preselected
- [x] 1.3 In `ActivitiesController` submit toolbar: same preselection and persistence behavior as 1.2; verify with a restart
- [x] 1.4 Add unit test covering selection fallback when stored id is unknown (both views default to first connector); run `./gradlew test --tests '*Config*' --tests '*ControllerTest*'`

## 2. Reliable Submit Notifications

- [x] 2.1 Store the primary `Stage` in `MainWindowController.show()` and use it as owner in `onUserNotifactionRequest` (`Notifications.create().owner(stage)`), guarded against a not-yet-showing stage; verify toast appears on submit from the Report view
- [x] 2.2 Add test verifying submit-result notification is invoked with owner window and on failure path; run `./gradlew test --tests '*MainWindowController*' --tests '*ActivitiesControllerTest*'`

## 3. Report View Immutability Indication

- [x] 3.1 In the report comment column cell factory (`ActivityTableCell`), apply strikethrough style class when all backing items are submitted (any connector); verify visually after submit and after restart
- [x] 3.2 Ensure checkbox column and comment column both re-read `SubmitStatusTracker` on `tableForReport.refresh()` triggered by `submitSelectedRows()` and `onItemsSubmitted`; verify in-session state update without restart
- [x] 3.3 Add/update test covering strikethrough style applied for fully-submitted rows and not applied for partially-submitted rows; run `./gradlew test --tests '*ReportBindingTest*' --tests '*ActivitiesControllerTest*'`

## 4. Activities View Selection Fix

- [x] 4.1 In `ActivitiesController.submitSelectedItems()`, replace `connector.javaClass.simpleName.lowercase()` with `connector.id` when reading selections; verify: check an item with tempo active, click Submit Selected, item is submitted and selection cleared
- [x] 4.2 Update `ActivitiesControllerTest` to cover selection lookup by connector id (checked item is found and submitted); run `./gradlew test --tests '*ActivitiesControllerTest*'`

## 5. Final Verification

- [x] 5.1 Run full `./gradlew test` and fix any regressions
- [ ] 5.2 Manual end-to-end: submit group from Report view to tempo → toast appears, report row struck through and checkbox disabled, Activities view shows disabled action buttons/checkbox immediately; restart → last-used connector preselected, report rows still struck through; run `./gradlew build`

## 6. Revisions: Reliable Persistence, Selector Sync, Lock Icon

- [x] 6.1 Add `save()` to `JsonConfigService`, `YamlConfigService`, and `ConfigServiceFacade`; change `persistLastUsedConnector` to save immediately on selection change; update `SubmitConnectorSelectionTest` to verify `save()` is invoked and id stored
- [x] 6.2 Add `ConnectorSelected` event and publish it from both views' combo listeners; add handlers that select the same connector in the other view (guarded against oscillation and uninitialized toolbars)
- [x] 6.3 Add `LOCK` glyph to `Glyph` enum; replace strikethrough in `ActivityTableCell` with the lock icon (grouped rows) and show it in ungrouped comment cells too
- [x] 6.4 Show the same lock icon in `TimeTrackingItemCellWithActions` for items submitted to any connector, inserted before the submit checkbox and removed when not submitted
- [ ] 6.5 Run full `./gradlew test` and `./gradlew build`; manual check: change connector in Report view → Activities view follows; restart → preselection survives; submitted items show lock icon in both views

## 7. Tempo Submission Refinements

- [x] 7.1 Accept lower case issue keys (`[A-Za-z]+-\d+`), normalize to upper case for issue resolution; add unit tests for key and comment extraction
- [x] 7.2 Use the text after the issue key as the Tempo worklog comment: add `description` to `TempoWorklogInput`, serialize it (JSON-escaped), and populate it from the activity text in ITEMS and GROUPED modes
- [x] 7.3 Lower the same-day guard: skip only when the same issue already has a worklog on that day (per issue+day check) instead of skipping whole days; in GROUPED mode the time cursor is not advanced by skipped groups
- [x] 7.4 Add `TempoSubmitConnectorTest` covering: lower case keys resolved via normalized key, worklog comment from text after key, same issue+day skipped, different issue on same day submitted; run full `./gradlew test`

## 8. Final Amendments

- [x] 8.1 Report view: show the lock icon next to the submit checkbox for fully submitted rows (like the Activities view) and widen the checkbox column accordingly
- [x] 8.2 UserNotifier: pre-wrap long notification messages at word boundaries so the ControlsFX notification bar keeps a bounded width and stays within the owner window
- [x] 8.3 Run full `./gradlew test` and `./gradlew build`; manual check: long skip messages stay inside the window; lock icon appears next to the report checkbox
