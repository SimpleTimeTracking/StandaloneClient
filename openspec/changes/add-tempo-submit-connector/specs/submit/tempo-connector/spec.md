# Tempo Connector Delta Spec

## Purpose

Lets users submit tracked time directly to Jira Tempo Cloud from the STT submit views, with issue-key mapping, configurable rounding, and safeguards against double-submission and modification of already-submitted items.

## ADDED Requirements

### Requirement: Tempo Cloud Authentication

The system SHALL authenticate against the Tempo Cloud worklog API using a Tempo user token (Bearer authentication) that the user generates in the Tempo UI, and SHALL reuse the credentials from the existing `jira:` config section for resolving issue keys and the author's account id.

#### Scenario: Tempo token configured
- **WHEN** the `tempo:` config section contains a valid `tempoToken`
- **THEN** worklog requests are sent to the Tempo Cloud API with the token as Bearer credential

#### Scenario: Tempo token not configured
- **WHEN** no `tempoToken` is present in the config
- **THEN** the connector is inert: it does not submit anything and reports that it is not configured when submission is attempted

#### Scenario: Shared Jira credentials resolve issue metadata
- **WHEN** an item's activity contains a Jira issue key
- **THEN** the connector resolves the issue and the author's account id using the existing Jira configuration (URI, username, token)

### Requirement: Activity-to-Issue Mapping

The system SHALL extract a Jira issue key from the item's activity text using the convention `^([A-Za-z]+-\d+)` (case-insensitive; the key is normalized to upper case) and SHALL NOT make the extraction rule configurable. The text after the issue key SHALL be used as the Tempo worklog comment.

#### Scenario: Activity starts with issue key
- **WHEN** an item's activity is `ACT-42: fix login bug`
- **THEN** the issue key `ACT-42` is used for the Tempo worklog and the worklog comment is `fix login bug`

#### Scenario: Lower case issue keys are accepted
- **WHEN** an item's activity starts with a lower case issue key such as `act-42: fix login bug`
- **THEN** the issue is resolved with the normalized key `ACT-42`

#### Scenario: Activity without comment text
- **WHEN** an item's activity consists only of an issue key
- **THEN** the worklog is created without a description

#### Scenario: Activity without issue key is skipped with warning
- **WHEN** an item's activity does not contain a resolvable issue key
- **THEN** the item is skipped, a warning is reported, and the item remains unmarked and submittable

### Requirement: No-End Items Are Never Submitted

The system SHALL refuse to submit any item without an end time.

#### Scenario: Ongoing item is skipped with warning
- **WHEN** a submission run includes an item without an end time
- **THEN** the item is skipped, a warning is reported, and the item remains unmarked

### Requirement: Zero-Duration Entries Are Never Submitted

The system SHALL refuse to submit items or groups whose duration rounds to zero under the configured rounding, in both individual and grouped submission.

#### Scenario: Item whose duration rounds to zero is skipped with warning
- **WHEN** a submission run includes an item whose duration rounds to zero
- **THEN** the item is skipped, a warning is reported, and the item remains unmarked and submittable

#### Scenario: Group whose duration rounds to zero is skipped with warning
- **WHEN** a grouped submission includes a report group whose duration rounds to zero
- **THEN** no worklog is created for the group, a warning is reported, and the group's items remain unmarked and submittable

#### Scenario: Sub-threshold entries of the same group are summed before rounding
- **WHEN** a grouped submission includes multiple items with the same comment whose durations individually round to zero but sum to a non-zero rounded duration
- **THEN** the group is submitted with the rounded sum as duration

### Requirement: Rounding Uses the Report Rounding Configuration

The system SHALL round start times, end times, and durations using the same rounding configuration as the report view (`report.roundDurationsTo`).

#### Scenario: Times rounded before submission
- **WHEN** an item with unrounded start/end times is submitted
- **THEN** the worklog start time and time spent are the rounded values

### Requirement: Individual Submission of Items

The system SHALL create one Tempo worklog per item, anchored at the item's rounded start time with the rounded duration.

#### Scenario: Multiple items produce multiple worklogs
- **WHEN** three items for the same issue are submitted
- **THEN** three worklogs are created, each with its own rounded start time and duration

### Requirement: Summary Submission Modes

The system SHALL support two configurable modes for `submitSummary`, selected by the `summaryMode` setting in the `tempo:` config section: `ITEMS` and `GROUPED`.

#### Scenario: ITEMS mode submits backing items individually
- **WHEN** `summaryMode` is `ITEMS` and report rows are submitted
- **THEN** the backing items of the selected rows are submitted one by one with their rounded real start and end times

#### Scenario: GROUPED mode synthesizes sequential times
- **WHEN** `summaryMode` is `GROUPED` and report rows are submitted
- **THEN** one worklog per report group is created, where the first group of a day starts at the configured `workdayStart` (default 08:00), each subsequent group starts where the previous group ends, and group durations are the rounded durations

#### Scenario: GROUPED mode orders groups by earliest real start
- **WHEN** `summaryMode` is `GROUPED` and groups would overlap or interleave in the real timeline
- **THEN** groups are ordered by the earliest start of their backing items, and synthesized times never overlap within a day

#### Scenario: GROUPED mode restarts per day
- **WHEN** `summaryMode` is `GROUPED` and the selection spans multiple days
- **THEN** each day's synthesized times restart at `workdayStart`

### Requirement: Issue-Level Pre-Check Against Tempo

The system SHALL query existing Tempo worklogs for the user and the selected days before submitting, and SHALL skip an item or report group only when the same Jira issue already has a Tempo worklog on that day. Worklogs of other issues on the same day do not block submission.

#### Scenario: Same issue already logged on that day
- **WHEN** at least one worklog exists in Tempo for the same issue on a selected day
- **THEN** items and report groups for that issue on that day are skipped, a warning is reported, and they remain unmarked and submittable

#### Scenario: Different issue on a day with existing worklogs
- **WHEN** a day already has worklogs in Tempo, but only for other issues
- **THEN** items and report groups for a different issue on that day are submitted normally

#### Scenario: GROUPED mode continues with the time cursor after a skipped group
- **WHEN** a grouped submission skips a report group because its issue was already logged on that day
- **THEN** the synthesized start time of the following group on that day is not advanced by the skipped group's duration

### Requirement: Submission Failure Reporting and Retry

The system SHALL mark each item as submitted only after its worklog was created successfully, and SHALL report a per-run summary (submitted, skipped, failed counts) to the user. Failed items remain submittable.

#### Scenario: Partial failure
- **WHEN** some worklogs are created successfully and some requests fail
- **THEN** only the successfully created worklogs are marked as submitted, and the run summary names the failures

#### Scenario: Failed items remain submittable
- **WHEN** a later submission run is started after a partial failure
- **THEN** the previously failed items are submitted again without re-creating worklogs for already-marked items

### Requirement: Immediate Persistence of Submission Marks

The system SHALL persist a submission mark immediately when it is made, so that a crash after successful submission cannot lose the mark.

#### Scenario: Mark survives crash after submission
- **WHEN** an item is submitted successfully and the application crashes before shutdown
- **THEN** the item is still marked as submitted on the next start
