# Proposal: add-tempo-submit-connector

## Why

Time tracked in STT currently leaves the app only as files (JSON/CSV) that must be imported elsewhere by hand. The team logs work in Jira Tempo (Cloud); a separate offline tool (`stt2jira`) bridges the gap, but it is disconnected from STT, has no submit-state awareness, and duplicates functionality. A Tempo connector inside STT closes the loop: select tracked items, submit them directly to Tempo, with submit status tracked so items are never double-booked or silently modified afterwards.

## What Changes

- Add a `TempoSubmitConnector` (id `tempo`) implementing the existing `SubmitConnector` interface, registered via Dagger `@IntoSet` multibinding.
- Talk to **Tempo Cloud** (`api.tempo.io/4` worklog API, Bearer auth with a Tempo user token generated in the Tempo UI). Jira credentials from the existing `jira:` config section are reused for issue lookup (`getIssue`) and fetching the author's `accountId` (new `myself` call).
- Extend `JiraClient` minimally: `getIssue` also returns the numeric issue id; new `myself()` to fetch the accountId.
- New `tempo:` config section (not part of `ConnectorConfig`): `tempoToken` (`PasswordSetting`, `!encrypted`), `workdayStart` (default 08:00), `summaryMode` (`ITEMS` | `GROUPED`).
- `submitItems`: one Tempo worklog per item, start/end rounded via the existing `DurationRounder` (fed by `report.roundDurationsTo`).
- `submitSummary`: configurable mode —
  - `ITEMS`: flatten the selected report rows' backing items and submit them one by one with rounded real times;
  - `GROUPED`: one worklog per report group; synthesized times restart at `workdayStart` per day, groups ordered by earliest backing item start, each next group starts where the previous (rounded duration) ends.
- Skip-with-warning rules (items stay submittable): activity without a resolvable Jira issue key; item without end time (never submittable in general).
- Idempotency (hybrid): hardened local status tracking **plus** a day-level Tempo pre-check — days that already have Tempo worklogs are skipped with a warning.
- `SubmitStatusTracker`: persist marks **immediately** on `markSubmitted`, not only on `stop()`.
- Enforce item immutability (`requireNotSubmitted`) for **all** connectors at the command-handler and UI level per the existing `submit/item-locking` spec (spec already exists; this change implements and verifies it).
- Failures are reported per-run via `NotifyUser` ("N submitted, M skipped, K failed"); failed items remain submittable.

Explicitly deferred: un-submit UI escape hatch (file editing of `~/.stt/submit-status` is accepted for now); enforcing edit-blocking in the CLI.

## Capabilities

### New Capabilities

- `submit/tempo-connector`: Submitting time tracking items to Jira Tempo Cloud via the Tempo worklog API — authentication, activity-to-issue mapping, rounding, ITEMS/GROUPED summary modes, skip rules, day-level pre-check, and failure reporting.

### Modified Capabilities

- `submit/status-tracking`: New requirement that submission marks are persisted immediately when made, so a crash after successful submission cannot lose the mark (previously persistence was only guaranteed on shutdown).

## Impact

- **New code**: `org.stt.submit.tempo` package (`TempoSubmitConnector`, Tempo worklog API client).
- **Modified code**: `org.stt.connector.jira.JiraClient` (issue id, `myself`), `org.stt.submit.SubmitStatusTracker` (immediate persistence), `org.stt.submit.SubmitModule` (new `@IntoSet` provider), `org.stt.config` (new `TempoConfig`, registered in `ConfigRoot`), command handler + UI cells implementing item-locking enforcement.
- **Config**: new `tempo:` YAML section; no changes to existing sections; `!encrypted` tag reused for the Tempo token.
- **Runtime behavior**: submit runs issue network requests (Jira + Tempo); edits of submitted items become rejected instead of silently applied; `~/.stt/submit-status` is written during operation, not only at shutdown.
- **No breaking config changes**: existing sections keep their shape; `tempo:` is optional (connector is inert if `tempoToken` is unset).
