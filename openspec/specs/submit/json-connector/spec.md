# JSON Connector Specification

## Purpose

Provides a JSON file connector as the default `SubmitConnector` implementation, writing time tracking items and summary reports to a configurable JSON output file.

## Requirements

### Requirement: JSON Output for Individual Items

The system SHALL serialize `TimeTrackingItem` objects to a JSON array with fields: `activity`, `start` (ISO-8601), `end` (ISO-8601 or null), and `submittedAt` (ISO-8601 timestamp of submission).

#### Scenario: Submit single item
- **WHEN** a single item is submitted
- **THEN** the output file contains a JSON array with one object containing all four fields

#### Scenario: Submit multiple items
- **WHEN** multiple items are submitted
- **THEN** the output file contains a JSON array with one object per item

#### Scenario: Item without end time
- **WHEN** an ongoing item (no end time) is submitted
- **THEN** the `end` field is serialized as `null`

### Requirement: JSON Output for Summary Reports

The system SHALL serialize `ReportListItem` objects to a JSON array with fields: `comment`, `isBreak`, `duration`, and `roundedDuration`.

#### Scenario: Submit summary items
- **WHEN** summary report items are submitted
- **THEN** the output file contains a JSON array with the summary fields

#### Scenario: Break item in summary
- **WHEN** a break item is included in the summary submission
- **THEN** the `isBreak` field is `true` for that item

### Requirement: Configurable Output Path

The connector SHALL support both absolute paths (starting with `/`) and paths relative to the application home directory.

#### Scenario: Absolute output path
- **WHEN** the configured file path starts with `/`
- **THEN** the connector writes to the absolute path directly

#### Scenario: Relative output path
- **WHEN** the configured file path does not start with `/`
- **THEN** the connector resolves the path relative to the application home directory

### Requirement: File Overwrite on Each Submit

The connector SHALL truncate and overwrite the output file on each submission, keeping only the latest submission data.

#### Scenario: Output file overwritten
- **WHEN** items are submitted a second time
- **THEN** the output file contains only the items from the second submission