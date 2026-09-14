# Tasks: add-tempo-submit-connector

## 1. Config & DI

- [ ] 1.1 Create `TempoConfig` (`tempoToken: PasswordSetting?`, `workdayStart: Duration`, `summaryMode: TempoSummaryMode` enum ITEMS/GROUPED) and register it in `ConfigRoot`; verify YAML round-trip keeps the section and `!encrypted` token (unit test on `YamlConfigService`)
- [ ] 1.2 Wire the `@IntoSet` provider for `TempoSubmitConnector` in `SubmitModule` (injects `TempoConfig`, `JiraConnector`, `DurationRounder`, event bus); verify Dagger build succeeds and `Set<SubmitConnector>` contains id `tempo`

## 2. JiraClient extensions

- [ ] 2.1 Extend `getIssue` parsing so `Issue` also carries the numeric issue id; update `JiraClientTest`/`Issue` usages and verify tests pass
- [ ] 2.2 Add `myself()` returning the account id (Basic auth, `rest/api/3/myself`), with unit test covering success and auth-failure paths

## 3. Tempo API client

- [ ] 3.1 Create `org.stt.submit.tempo` client: `POST /worklogs` (`authorAccountId`, `issueId`, `startDate`, `startTime`, `timeSpentSeconds`) and `GET /worklogs` by user + date range, Bearer-authed with the Tempo token via `java.net.http`; unit tests for request shaping and error mapping (401/403/404/5xx)
- [ ] 3.2 Map HTTP failures to typed exceptions and verify no partial state is exposed to callers (unit test)

## 4. TempoSubmitConnector

- [ ] 4.1 Implement issue-key extraction (`^([A-Z]+-\d+)`) and skip-with-warning path (event bus `NotifyUser`), plus the no-end-time exclusion; unit tests for both skip rules and that skipped items are never marked
- [ ] 4.2 Implement `submitItems`: per-item worklog with `DurationRounder`-rounded start/end, mark + persist after each success; unit tests with mocked Tempo client (success, partial failure, retry leaves marked items untouched)
- [ ] 4.3 Implement `submitSummary` ITEMS mode: flatten selected rows' backing items and delegate to the item pipeline; unit test verifying rounded real times are used
- [ ] 4.4 Implement `submitSummary` GROUPED mode: per-day synthesis (first group at `workdayStart`, sequential rounded durations, order by earliest backing item start); unit tests covering interleaving, multi-day restart, and overlap-free output
- [ ] 4.5 Implement the day-level pre-check: query existing Tempo worklogs for the selected days and skip occupied days with a warning; unit test for occupied/empty day handling
- [ ] 4.6 Implement the per-run summary notification (submitted / skipped / failed counts and reasons) via `NotifyUser`; unit test verifying counts for a mixed run
- [ ] 4.7 Verify inert behavior when `tempoToken` is unset (submission reports not-configured, no network calls); unit test

## 5. Status tracker hardening

- [ ] 5.1 Change `SubmitStatusTracker` to persist each mark immediately on `markSubmitted` (not only in `stop()`); unit test simulating a crash after mark (no stop, reload file, mark present)

## 6. Item immutability enforcement (existing item-locking spec)

- [ ] 6.1 Call `requireNotSubmitted` in `Activities` command handler paths (`addNewActivity`, `endCurrentActivity`, `removeActivity`, `removeActivityAndCloseGap`, `bulkChangeActivity`); unit tests per blocked path and the resume-is-allowed path
- [ ] 6.2 Disable edit/delete/continue/stop actions in `TimeTrackingItemCellWithActions` for items submitted to any connector; unit tests for enabled/disabled states
- [ ] 6.3 Surface the `IllegalStateException` from blocked mutations as a `NotifyUser` message; manual check in the running GUI

## 7. Integration verification

- [ ] 7.1 Verify the connector appears in the Report/Activities connector combos and that a dry-run against a real Tempo instance (test project, one day) creates the expected worklogs; clean up created test worklogs afterwards
- [ ] 7.2 Run `./gradlew build` (compile + all tests) and confirm green on macOS, Linux, and Windows-compatible test paths
