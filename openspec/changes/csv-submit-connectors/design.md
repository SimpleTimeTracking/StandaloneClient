## Context

The existing submit framework provides a `SubmitConnector` interface, Dagger `@IntoSet` discovery, content-based item status tracking, selection management, and one concrete connector (`JsonSubmitConnector`). The JSON connector serializes items as a flat JSON array — no pivot table, no per-day column layout, no duration splitting.

This design adds two CSV connectors that produce pivot tables with per-day columns. See `proposal.md` for motivation and capability scope; see spec files for behavioral requirements.

## Goals / Non-Goals

**Goals:**
- Two `SubmitConnector` implementations: CsvItemsSubmitConnector and CsvSummarySubmitConnector
- Both handle both `submitItems()` and `submitSummary()` entry points
- Pivot-table CSV format with one column per day (only days with tracked time)
- Midnight-spanning items split across day columns
- Per-day duration aggregation in the summary connector
- Hardcoded default output paths: `~/.stt/submit-items.csv` and `~/.stt/submit-summary.csv`
- Hook into existing locking, selection, cross-view sync infrastructure

**Non-Goals:**
- Configurable output paths, delimiters, or date formats (future if needed)
- CSV import — this is export-only
- CLI submit commands
- Streaming or incremental writes — snapshot model only
- Non-pivot-table CSV formats (flat row-per-item-with-date-column could be a future connector)

## Decisions

### Decision 1: Pivot Table Format with Per-Day Columns

Both CSV connectors produce a pivot table where day boundaries are columns, not a flat "one row per item with a date column".

**Rationale:** The pivot format is the most useful for spreadsheet consumption — users can sum across columns, visually scan time per day, and it mirrors how time tracking data is reported. The JSON connector already provides a flat format; there's no need to duplicate it in CSV.

**Alternatives considered:** Flat CSV with a `date` column (simpler code, but less useful in spreadsheets). Two outputs per connector (pivot + flat). Single unified CSV with both formats.

### Decision 2: Per-Day Duration Splitting Across Midnight

When a `TimeTrackingItem` spans midnight, `CsvItemsSubmitConnector` splits the duration into the two day columns. `CsvSummarySubmitConnector` splits first, then aggregates per day.

**Rationale:** Crediting a midnight-spanning item entirely to the start day misrepresents the actual time worked. A 22:00-02:00 shift is 2 hours on day 1 and 2 hours on day 2. Splitting is more accurate for overtime and per-day reporting.

**Implementation:** `Duration.between(start, dayEnd)` for day 1, `Duration.between(dayStart, end)` for day 2, where `dayEnd = start.toLocalDate().plusDays(1).atStartOfDay()` and `dayStart = dayEnd`.

**Alternatives considered:** Credit to start day only (simpler, but inaccurate for cross-midnight work). Credit to end day only (same problem, opposite direction).

### Decision 3: Day-Column Set Derived from Data

Columns are determined by scanning all submitted items for distinct days that have at least one tracked minute. No empty columns are emitted.

**Rationale:** A pivot table with 180 columns for 6 months of sparse data is unusable. Only days that actually have tracked time produce a column.

**Implementation:** Collect `sortedSetOf<LocalDate>()` from all items, iterating across each item's day range (split across midnight if needed), then header row is `activity` + each date formatted as `yyyy-MM-dd`.

### Decision 4: CSV Serialization Built-In (No External Library)

Both connectors build CSV output using `StringBuilder` with proper escaping (double-quote wrapping for values containing commas, quotes, or newlines).

**Rationale:** The CSV format is simple. Adding a CSV library (OpenCSV, Apache Commons CSV) adds a dependency for a small amount of code. The existing `JsonSubmitConnector` already hand-builds its output; following the same pattern keeps the codebase consistent.

**Alternatives considered:** OpenCSV library (cleaner edge-case handling, but new dependency). Apache Commons CSV (same).

### Decision 5: No Config — Hardcoded Defaults

Both connectors use hardcoded default output paths. No YAML config block, no `ConnectorConfig` parameters for delimiter, path, or date format.

**Rationale:** The JSON connector already demonstrates config-driven paths, but for this change the user explicitly asked for hardcoded defaults. Config support can be added later if needed.

**Implementation:** `submit-items.csv` relative to the home path (same pattern as `JsonSubmitConnector` with a relative path). The connector is created in `SubmitModule` with a default `ConnectorConfig(type = "csv-items", file = ".stt/submit-items.csv")` to use the existing resolution logic, or with its own hardcoded path.

### Decision 6: Per-Day Aggregation in Summary Connector

`CsvSummarySubmitConnector` aggregates durations per comment per day, not just per comment. Each cell in the pivot table is the sum of all `TimeTrackingItem` durations for that comment on that day.

**Rationale:** The Report view groups by comment but the resulting pivot table shows time per day, not just total per comment. A single cell per comment would lose the per-day granularity.

**Implementation:** For `submitItems()` (Activities view entry point), group raw items by comment, then for each comment group by day (after splitting midnight spans), sum durations per day. For `submitSummary()` (Report view entry point), unpack `ReportListItem.backingItems` and apply the same per-day logic.

## Risks / Trade-offs

- **Risk:** Midnight-split logic is duplicated across both connectors. **Mitigation:** Extract per-day duration computation into a shared utility function, tested independently.
- **Risk:** Hand-rolled CSV escaping misses an edge case (e.g., value containing `"` or newline). **Mitigation:** Write a comprehensive test for escaping; the format is well-defined and simple.
- **Trade-off:** Hardcoded paths mean users cannot change the output location without code changes. Acceptable for now — config can be added as a follow-up.
- **Trade-off:** Snapshot model overwrites the CSV each time, losing any external edits. Same as JSON connector — consistent behavior across all connectors.