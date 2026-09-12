## Purpose

Provides a CSV file connector for submitting grouped summary time entries as a pivot table with one row per grouped comment and one column per day, matching the Report view's grouping logic.

## ADDED Requirements

### Requirement: CSV Output for Summary Items

The system SHALL serialize grouped summary items to a CSV file in pivot-table format: one row per grouped comment, day-columns between the earliest and latest tracked day, with duration values in `HH:mm` format.

#### Scenario: Single summary row written as CSV row
- **WHEN** a single grouped comment is submitted
- **THEN** the output CSV contains a header row with `comment` and one column per day (with tracked time), and a data row with the comment text and its summed duration in the appropriate day column

#### Scenario: Multiple summary rows
- **WHEN** multiple grouped comments are submitted
- **THEN** the output CSV contains one row per comment, each with duration in the appropriate day columns

#### Scenario: Duration aggregation per day
- **WHEN** multiple items for the same comment exist on the same day
- **THEN** their durations are summed into a single cell for that day column

#### Scenario: Duration format
- **WHEN** a grouped comment has a total duration of 3 hours and 45 minutes on a day
- **THEN** the cell value in the CSV is `03:45`

#### Scenario: Only days with tracked time appear
- **WHEN** items exist on 2026-08-31 and 2026-09-03 but not on 2026-09-01 or 2026-09-02
- **THEN** the CSV columns are only `comment`, `2026-08-31`, and `2026-09-03`

#### Scenario: Midnight split handled
- **WHEN** items for a grouped comment span midnight
- **THEN** the duration is split per day before aggregation, so the portion is credited to the correct day column

### Requirement: Configurable Delimiter

The connector SHALL use comma (`,`) as the default CSV delimiter.

#### Scenario: Default delimiter is comma
- **WHEN** a CSV file is written
- **THEN** fields are separated by commas

### Requirement: Date Column Format

The system SHALL format day column headers as `yyyy-MM-dd`.

#### Scenario: Column header format
- **WHEN** items are tracked on August 31, 2026
- **THEN** the column header is `2026-08-31`

### Requirement: Default Output Path

The connector SHALL write to `~/.stt/submit-summary.csv` by default, with no configuration required.

#### Scenario: Default output file
- **WHEN** the csv-summary connector submits items
- **THEN** the output file is `submit-summary.csv` in the `~/.stt/` directory

### Requirement: File Overwrite on Each Submit

The connector SHALL truncate and overwrite the output file on each submission, keeping only the latest submission data.

#### Scenario: Output file overwritten
- **WHEN** grouped items are submitted a second time
- **THEN** the output CSV contains only the items from the second submission

### Requirement: Both Submit Entry Points

The connector SHALL handle both `submitItems()` and `submitSummary()` entry points. When called via `submitItems()`, it SHALL internally group the raw `TimeTrackingItem`s by comment and per-day duration, producing the same grouping as the Report view.

#### Scenario: Submit from Activities view
- **WHEN** the connector receives a `submitItems()` call from the Activities view
- **THEN** it internally groups raw items by comment, aggregates per-day durations, and writes the grouped pivot table

#### Scenario: Submit from Report view
- **WHEN** the connector receives a `submitSummary()` call from the Report view
- **THEN** it uses the provided `ReportListItem` objects (already grouped) and aggregates their per-day backing item durations

### Requirement: Item Locking After Submit

The connector SHALL mark items as submitted via `SubmitStatusTracker.markSubmitted()` after each successful submit, making them immutable.

#### Scenario: Items locked after CSV summary submit
- **WHEN** items are submitted via the csv-summary connector
- **THEN** they are marked as submitted and cannot be modified

### Requirement: Cross-View Sync After Submit

The connector SHALL publish an `ItemsSubmitted` event after successful submit, triggering refresh in both views.

#### Scenario: Event published on submit
- **WHEN** items are submitted via the csv-summary connector
- **THEN** an `ItemsSubmitted` event is published on the event bus