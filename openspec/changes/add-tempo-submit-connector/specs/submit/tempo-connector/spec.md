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

The system SHALL extract a Jira issue key from the item's activity text using the convention `^([A-Z]+-\d+)` and SHALL NOT make the extraction rule configurable.

#### Scenario: Activity starts with issue key
- **WHEN** an item's activity is `ACT-42: fix login bug`
- **THEN** the issue key `ACT-42` is used for the Tempo worklog

#### Scenario: Activity without issue key is skipped with warning
- **WHEN** an item's activity does not contain a resolvable issue key
- **THEN** the item is skipped, a warning is reported, and the item remains unmarked and submittable

### Requirement: No-End Items Are Never Submitted

The system SHALL refuse to submit any item without an end time.

#### Scenario: Ongoing item is skipped with warning
- **WHEN** a submission run includes an item without an end time
- **THEN** the item is skipped, a warning is reported, and the item remains unmarked

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

### Requirement: Day-Level Pre-Check Against Tempo

The system SHALL query existing Tempo worklogs for the user and the selected days before submitting, and SHALL skip items on days that already have any Tempo worklog.

#### Scenario: Day already has Tempo worklogs
- **WHEN** at least one worklog exists in Tempo for a selected day
- **THEN** all items of that day are skipped, a warning is reported, and the items remain unmarked and submittable

#### Scenario: Day without existing worklogs
- **WHEN** no worklog exists in Tempo for a selected day
- **THEN** items of that day are submitted normally

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
