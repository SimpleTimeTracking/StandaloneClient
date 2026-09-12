## Why

The existing submit framework has only a JSON connector. Users need CSV output for importing time data into spreadsheets and other tools. Two CSV connectors — one for individual time tracking items and one for grouped summary rows — provide the most useful export formats for this use case.

## What Changes

- New `CsvItemsSubmitConnector` — writes individual `TimeTrackingItem`s to a CSV file as a pivot table with one column per day
- New `CsvSummarySubmitConnector` — writes grouped summary `ReportingItem`s to a CSV file as a pivot table with one column per day
- Both connectors handle both `submitItems()` and `submitSummary()` entry points intelligently:
  - csv-items expands summary items back into individual rows when called from Report view
  - csv-summary groups raw items internally when called from Activities view
- Both connectors use hardcoded default output paths (no config needed):
  - csv-items → `~/.stt/submit-items.csv`
  - csv-summary → `~/.stt/submit-summary.csv`
- Snapshot overwrite on each submit (same model as JSON connector)
- Items become immutable after CSV submit (consistent with existing behavior)
- Both connectors selectable in both Activities view and Report view

## Capabilities

### New Capabilities

- `submit/csv-items-connector`: Per-item CSV connector writing a pivot table with one row per time tracking item, one column per day (with tracked time), split durations across midnight, duration format HH:mm
- `submit/csv-summary-connector`: Grouped summary CSV connector writing a pivot table with one row per grouped comment, one column per day (with tracked time), duration format HH:mm

### Modified Capabilities

- None

## Impact

- Two new connector classes in `org.stt.submit.csv` package
- Both wired via Dagger `@IntoSet` in `SubmitModule`
- No changes to existing connectors, status tracking, selection management, or UI
- No changes to `ConnectorConfig` or YAML config