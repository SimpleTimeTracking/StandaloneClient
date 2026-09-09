## 1. Shared CSV Utility

- [x] 1.1 Create `CsvUtil.kt` with CSV cell escaping (quote values containing commas, quotes, or newlines) and verify with unit tests covering all edge cases
- [x] 1.2 Create shared per-day duration splitting utility (split `TimeTrackingItem` duration into per-day portions, handling midnight boundaries) and verify with tests for same-day, cross-midnight, and multi-day items

## 2. CsvItemsSubmitConnector

- [x] 2.1 Create `CsvItemsSubmitConnector` implementing `SubmitConnector` with id `"csv-items"`, writing to `~/.stt/submit-items.csv`, and verify the class compiles and implements the interface
- [x] 2.2 Implement `submitItems()`: build pivot table from raw `TimeTrackingItem`s — scan items for distinct days, write header row (activity + date columns), write one row per item with duration in correct day column — verify with a test that reads back the CSV content
- [x] 2.3 Implement midnight-split in items connector: verify a 22:00-02:00 item produces 02:00 in the start-day column and 02:00 in the next-day column
- [x] 2.4 Implement `submitSummary()` entry point: expand `ReportListItem.backingItems` into individual items and apply same pivot-table logic — verify with a test that submits grouped items and gets correct per-item rows
- [x] 2.5 Wire csv-items connector into `SubmitModule` via `@IntoSet` and verify it appears in the connector dropdown

## 3. CsvSummarySubmitConnector

- [x] 3.1 Create `CsvSummarySubmitConnector` implementing `SubmitConnector` with id `"csv-summary"`, writing to `~/.stt/submit-summary.csv`, and verify the class compiles and implements the interface
- [x] 3.2 Implement `submitSummary()`: build pivot table from `ReportListItem`s — aggregate per-comment per-day durations across backing items (after splitting), write one row per comment — verify with a test that reads back the CSV
- [x] 3.3 Implement `submitItems()` entry point: group raw items by comment, aggregate per-day durations, write the grouped pivot table — verify with a test that raw items produce correctly grouped CSV output
- [x] 3.4 Wire csv-summary connector into `SubmitModule` via `@IntoSet` and verify it appears in the connector dropdown

## 4. Integration with Existing Infrastructure

- [x] 4.1 Verify both connectors call `SubmitStatusTracker.markSubmitted()` after each successful submit, preventing item modification — verify with existing locking tests
- [x] 4.2 Verify both connectors publish `ItemsSubmitted` event after submit, triggering cross-view sync — verify with event bus test
- [x] 4.3 Verify both connectors are selectable in both Activities view and Report view connector dropdowns — manual test or integration test