# Tasks: add-tempo-submit-connector

## 1. Config & DI

- [x] 1.1 Create `TempoConfig` (`tempoToken: PasswordSetting?`, `workdayStart: Duration`, `summaryMode: TempoSummaryMode` enum ITEMS/GROUPED) and register it in `ConfigRoot`; verify YAML round-trip keeps the section and `!encrypted` token (unit test on `YamlConfigService`)
- [x] 1.2 Wire the `@IntoSet` provider for `TempoSubmitConnector` in `SubmitModule` (injects `TempoConfig`, `JiraConnector`, `DurationRounder`, event bus); verify Dagger build succeeds and `Set<SubmitConnector>` contains id `tempo`

## 2. JiraClient extensions

- [x] 2.1 Extend `getIssue` parsing so `Issue` also carries the numeric issue id; update `JiraClientTest`/`Issue` usages and verify tests pass
- [x] 2.2 Add `myself()` returning the account id (Basic auth, `rest/api/3/myself`), with unit test covering success and auth-failure paths

## 3. Tempo API client

- [x] 3.1 Create `org.stt.submit.tempo` client: `POST /worklogs` (`authorAccountId`, `issueId`, `startDate`, `startTime`, `timeSpentSeconds`) and `GET /worklogs` by user + date range, Bearer-authed with the Tempo token via `java.net.http`; unit tests for request shaping and error mapping (401/403/404/5xx)
- [x] 3.2 Map HTTP failures to typed exceptions and verify no partial state is exposed to callers (unit test)

## 4. TempoSubmitConnector

- [x] 4.1 Implement issue-key extraction (`^([A-Z]+-\d+)`) and skip-with-warning path (event bus `NotifyUser`), plus the no-end-time exclusion; unit tests for both skip rules and that skipped items are never marked
- [x] 4.2 Implement `submitItems`: per-item worklog with `DurationRounder`-rounded start/end, mark + persist after each success; unit tests with mocked Tempo client (success, partial failure, retry leaves marked items untouched)
- [x] 4.3 Implement `submitSummary` ITEMS mode: flatten selected rows' backing items and delegate to the item pipeline; unit test verifying rounded real times are used
- [x] 4.4 Implement `submitSummary` GROUPED mode: per-day synthesis (first group at `workdayStart`, sequential rounded durations, order by earliest backing item start); unit tests covering interleaving, multi-day restart, and overlap-free output
- [x] 4.5 Implement the day-level pre-check: query existing Tempo worklogs for the selected days and skip occupied days with a warning; unit test for occupied/empty day handling
- [x] 4.6 Implement the per-run summary notification (submitted / skipped / failed counts and reasons) via `NotifyUser`; unit test verifying counts for a mixed run
- [x] 4.7 Verify inert behavior when `tempoToken` is unset (submission reports not-configured, no network calls); unit test

## 5. Status tracker hardening

- [x] 5.1 Change `SubmitStatusTracker` to persist each mark immediately on `markSubmitted` (not only in `stop()`); unit test simulating a crash after mark (no stop, reload file, mark present)

## 6. Item immutability enforcement (existing item-locking spec)

- [x] 6.1 Call `requireNotSubmitted` in `Activities` command handler paths (`addNewActivity`, `endCurrentActivity`, `removeActivity`, `removeActivityAndCloseGap`, `bulkChangeActivity`); unit tests per blocked path and the resume-is-allowed path
- [x] 6.2 Disable edit/delete/continue/stop actions in `TimeTrackingItemCellWithActions` for items submitted to any connector; unit tests for enabled/disabled states
- [ ] 6.3 Surface the `IllegalStateException` from blocked mutations as a `NotifyUser` message; manual check in the running GUI

## 7. Integration verification

- [ ] 7.1 Verify the connector appears in the Report/Activities connector combos and that a dry-run against a real Tempo instance (test project, one day) creates the expected worklogs; clean up created test worklogs afterwards
- [x] 7.2 Run `./gradlew build` (compile + all tests) and confirm green on macOS, Linux, and Windows-compatible test paths

## 8. Bugfixes found during integration verification

- [x] 8.1 Tempo auth header was Base64-mangled (`Bearer <base64(token)>`), so Tempo returned 401 for every request ("You don't have permission to ..."); send the raw token (`Bearer <token>`), regression-tested in `TempoApiClientTest` via captured `Authorization` headers
- [x] 8.2 `JsonConfigService` silently reset the whole config when `stt.json` contained an empty list setting (jsoniter calls `clear()` on Kotlin's immutable `emptyList()` defaults); default `CommonPrefixGrouperConfig.baseLine` and `SubmitConfig.connectors` to mutable lists, regression-tested in `TempoTokenRoundTripTest`
- [x] 8.3 Views marked ALL selected items as submitted after calling a connector, so Tempo items that failed or were skipped (no issue key, zero duration, occupied day) got locked in `submit-status`; connectors now own submission marking (`CsvItemsSubmitConnector`, `CsvSummarySubmitConnector`, `JsonSubmitConnector` mark on successful write, Tempo marks per item), controllers no longer mark, delta spec updated (status-tracking: "Connectors Own Submission Marking"), regression-tested per connector (marks on success, no marks on failed write)
- [x] 8.4 GROUPED summary mode violated "No-End Items Are Never Submitted": `submitGrouped` submitted report rows whose backing items had no end time (using the row duration); groups containing ongoing items are now refused entirely with a user notification and remain unmarked; the ITEMS mode no-end skip now also notifies the user; regression-tested in `TempoSubmitConnectorTest`
- [x] 8.5 Report submit was a silent no-op: `latestReport` was only set by a change listener registered after the report binding's initial computation, so the `latestReport ?: return` guard always fired; the controller now reads the current value from the report binding at submit time. Report view now shows popups for "nothing selected" / "all already submitted" / failures; occupied-day skips notify the user
