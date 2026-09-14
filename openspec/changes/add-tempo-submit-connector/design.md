# Design: add-tempo-submit-connector

## Context

The submit infrastructure (`SubmitConnector` interface, Dagger `@IntoSet` discovery, `SubmitStatusTracker`, UI connector combo boxes) already exists with three file-based connectors. The existing `JiraConnector`/`JiraClient` do read-only issue lookup with Basic auth. A proven reference implementation for the Tempo Cloud API exists in the offline tool `stt2jira` (Rust, `/Users/roka/Development/actico/stt2jira`): it documents the two-token auth model, the `POST /worklogs` payload (`authorAccountId`, numeric `issueId`, `startDate`, `startTime`, `timeSpentSeconds`), and a day-level dedup strategy. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**

- One Tempo worklog per STT item, with STT as the single source of what is submitted and `SubmitStatusTracker` as the guard against duplication.
- Rounding identical to the report view (single `DurationRounder` instance).
- Contained config growth: a new `tempo:` section, existing sections untouched.

**Non-Goals:**

- Un-submit UI; editing `~/.stt/submit-status` by hand is the accepted escape hatch.
- Enforcing edit-blocking in the CLI (GUI + command handler paths only; CLI is a known gap).
- Worklog update/delete sync (STT never modifies or removes Tempo worklogs once created).
- Worklog attributes, billable seconds, remaining estimate — Tempo defaults are used.

## Decisions

### D1: Tempo Cloud with dedicated Tempo user token

Tempo Cloud (`api.tempo.io/4`) with Bearer auth via a Tempo user token generated in the Tempo UI. The embedded Tempo Timesheets endpoint was rejected: it reuses Jira credentials but is deprecated, and the team already has Cloud experience from `stt2jira`. Jira credentials from the existing `jira:` section are shared for issue resolution (and the author `accountId`); they cannot authenticate the Tempo API itself, hence exactly one new secret (`tempoToken`, `PasswordSetting` with the existing `!encrypted` tag).

### D2: Extend JiraClient, do not duplicate it

`TempoSubmitConnector` reuses `JiraConnector`/`JiraClient`. Two small additions: `getIssue` returns the numeric issue id alongside the summary (Tempo takes `issueId`, not the key), and a new `myself()` call fetches the author's `accountId` (fetched lazily and cached for the session). Alternative rejected: a second HTTP client for Tempo only — Tempo's bearer token and Jira's basic auth are separate anyway, so the Tempo API client is a new small class in `org.stt.submit.tempo` using `java.net.http` like `JiraClient`.

### D3: Dedicated `tempo:` config section instead of extending ConnectorConfig

`ConnectorConfig` stays `type` + `file`. The tempo connector reads a new top-level `tempo:` section (`TempoConfig: tempoToken, workdayStart, summaryMode`), mirroring how `jira:` already works. Rationale: `YamlConfigService` maps `ConfigRoot` by property paths and is bad at polymorphic lists; a sealed `ConnectorConfig` hierarchy would fight the loader. A dedicated section also means file connectors never see tempo fields. The `@IntoSet` provider in `SubmitModule` injects `TempoConfig`, `JiraConnector`, and `DurationRounder`. Inert by default: without `tempoToken` the connector exists (visible in the UI combo) but reports "not configured" on submit.

### D4: Rounding via the shared DurationRounder

Times and durations are rounded with the DI-provided `DurationRounder` fed by `report.roundDurationsTo`. No separate rounding constant; report and submissions cannot diverge.

### D5: GROUPED mode time synthesis

Groups = report rows (already grouped by comment). For each day in the selection: order groups by the earliest real start of their backing items; group *k* starts at `workdayStart` + sum of rounded durations of groups 0..k-1; duration = sum of the group's backing items' rounded durations. This never overlaps within a day and restarts per day. Interleaved activities (A, B, A) collapse into their groups — the real interleaving is intentionally not reproduced, only the order.

### D6: Hybrid idempotency — local marks + day-level Tempo pre-check

Submit run flow: query existing Tempo worklogs for the user over the selected days (one GET), skip all items on days that already have any worklog (warn), then POST remaining items in order, marking each item submitted immediately after its successful POST and persisting the mark right away. The pre-check covers drift where local state is missing but Tempo has data (manual logging, second machine, reset status file); the local marks cover the reverse direction (Tempo has nothing, e.g. worklog deleted remotely). Day-level granularity was chosen over item-level matching because item-level matching against Tempo worklogs (by description/start) is fragile, and the day-level rule is the proven `stt2jira` behavior.

### D7: Failure handling per item, never mark on failure

Each POST is independent; a failure does not abort the run. Marks are only written after confirmed success. At the end, one `NotifyUser` summary reports submitted / skipped (day occupied) / skipped (no issue key, no end) / failed counts, so failed and skipped items remain visibly submittable in the UI.

### D8: Item immutability enforced globally (implement existing spec)

`submit/item-locking` already specifies blocking edits of submitted items at the command handler (`Activities`: `addNewActivity`, `endCurrentActivity`, `removeActivity`, `removeActivityAndCloseGap`, `bulkChangeActivity` call `requireNotSubmitted`) and UI level (disabled action buttons). This change implements that spec as-is; it applies to all connectors because the tracker is connector-agnostic. CLI enforcement is a known gap (non-goal).

## Risks / Trade-offs

- [Day-level pre-check is coarse: a day with one manually logged 10-min entry blocks all STT items for that day] → Warning names the occupied days; user resolves in Tempo (delete or keep). Accepted because false-positive duplicates are worse than re-running after cleanup.
- [Local marks can be stale if a Tempo worklog is deleted remotely — item stays blocked] → Accepted for now; escape hatch is editing `~/.stt/submit-status` by hand. An un-submit UI remains a possible follow-up.
- [Crash between Tempo POST success and mark persistence] → Mitigated by the day-level pre-check: a resubmission run will skip the whole day because Tempo already has worklogs.
- [Multi-worklog runs are not transactional] → Accepted; per-item marking plus retry semantics keep the outcome consistent.
- [`SubmitStatusTracker` becomes load-bearing for edit-blocking] → Immediate persistence (D6/D8) and its plain-text, content-keyed format keep it inspectable and recoverable.
- [Tempo API contract drift] → Contained in one small client class; integration points match `stt2jira` usage (v4 worklog API).

## Migration Plan

No data migration. New `tempo:` section is optional; existing configs load unchanged (`skipMissingProperties` loader). Rollback = remove the connector registration; file connectors and all existing behavior are untouched. Users needing Tempo credentials: create a Jira API token (existing flow) and a Tempo user token in the Tempo UI.

## Open Questions

None — deferred items (un-submit UI, CLI enforcement) are recorded as non-goals.
