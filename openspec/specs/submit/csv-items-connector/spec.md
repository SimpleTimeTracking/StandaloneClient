# CSV Items Connector Specification

## Purpose

Provides a CSV file connector for submitting individual time tracking items as a pivot table with one row per item and one column per day, enabling easy import into spreadsheet applications.

## Requirements

### Requirement: CSV Output for Individual Items

The system SHALL serialize `TimeTrackingItem` objects to a CSV file in pivot-table format: one row per item, day-columns between the earliest and latest tracked day, with duration values in `HH:mm` format.

#### Scenario: Single item written as CSV row
- **WHEN** a single item is submitted
- **THEN** the output CSV contains a header row with `activity` and one column per day (with tracked time), and a data row with the item's comment and its duration in the appropriate day column

#### Scenario: Multiple items on same day
- **WHEN** multiple items on the same day are submitted
- **THEN** the output CSV contains one row per item, each with its duration in the shared day column

#### Scenario: Items spanning midnight
- **WHEN** an item spans midnight (e.g., 22:00 to 02:00)
- **THEN** the duration is split across the two day columns: the portion on the start day in the start-day column, and the portion after midnight in the next-day column

#### Scenario: Duration format
- **WHEN** an item has a duration of 1 hour and 30 minutes
- **THEN** the cell value in the CSV is `01:30`

#### Scenario: Only days with tracked time appear
- **WHEN** items exist on 2026-08-31 and 2026-09-03 but not on 2026-09-01 or 2026-09-02
- **THEN** the CSV columns are only `activity`, `2026-08-31`, and `2026-09-03`

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

The connector SHALL write to `~/.stt/submit-items.csv` by default, with no configuration required.

#### Scenario: Default output file
- **WHEN** the csv-items connector submits items
- **THEN** the output file is `submit-items.csv` in the `~/.stt/` directory

### Requirement: File Overwrite on Each Submit

The connector SHALL truncate and overwrite the output file on each submission, keeping only the latest submission data.

#### Scenario: Output file overwritten
- **WHEN** items are submitted a second time
- **THEN** the output CSV contains only the items from the second submission

### Requirement: Both Submit Entry Points

The connector SHALL handle both `submitItems()` and `submitSummary()` entry points. When called via `submitSummary()`, it SHALL expand each `ReportListItem.backingItems` back into individual `TimeTrackingItem` rows.

#### Scenario: Submit from Report view
- **WHEN** the connector receives a `submitSummary()` call from the Report view
- **THEN** it expands the backing items of each selected report row and writes them as individual item rows

#### Scenario: Submit from Activities view
- **WHEN** the connector receives a `submitItems()` call from the Activities view
- **THEN** it writes the raw items directly as individual item rows

### Requirement: Item Locking After Submit

The connector SHALL mark items as submitted via `SubmitStatusTracker.markSubmitted()` after each successful submit, making them immutable.

#### Scenario: Items locked after CSV submit
- **WHEN** items are submitted via the csv-items connector
- **THEN** they are marked as submitted and cannot be modified

### Requirement: Cross-View Sync After Submit

The connector SHALL publish an `ItemsSubmitted` event after successful submit, triggering refresh in both views.

#### Scenario: Event published on submit
- **WHEN** items are submitted via the csv-items connector
- **THEN** an `ItemsSubmitted` event is published on the event bus
