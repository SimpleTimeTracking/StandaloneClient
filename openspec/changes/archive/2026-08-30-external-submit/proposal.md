## Why

The submit feature — a pluggable connector architecture for submitting time tracking entries to external systems — has been fully implemented in the codebase but lacks formal OpenSpec documentation. This change captures the existing design, requirements, and implementation details as authoritative OpenSpec artifacts so the feature is documented, testable, and extensible.

## What Changes

- **No behavioral changes** — this is pure documentation of existing functionality
- New delta specs created under `specs/submit/` capturing all implemented requirements
- `doc/stories/submit/plan.md` superseded by OpenSpec artifacts; retained as historical reference

## Capabilities

### New Capabilities

- `submit/connector-architecture`: Pluggable connector interface (`SubmitConnector`), config model (`SubmitConfig`, `ConnectorConfig`), Dagger `@IntoSet` binding via `SubmitModule`, and YAML configuration
- `submit/status-tracking`: Per-item per-connector submit status persisted to `~/.stt/submit-status` via `SubmitStatusTracker`; content-based item keying (Base64-encoded `activity|start|end`)
- `submit/selection-management`: Per-connector checkbox selection state via `SubmitSelectionManager`; memory-only, isolated by connector ID
- `submit/json-connector`: Concrete `JsonSubmitConnector` writing items to a JSON file; supports absolute and relative output paths; serializes both individual items and summary reports
- `submit/item-locking`: Submitted items become immutable; enforced at two levels — `Activities` command handler rejects mutations via `requireNotSubmitted()`, and `TimeTrackingItemCellWithActions` UI disables action buttons
- `submit/ui-activities-view`: Connector ComboBox, per-cell submit checkbox, and "Submit Selected" button in the Activities view toolbar
- `submit/ui-report-view`: Connector ComboBox, checkbox column with three-state (all/some/none submitted), and "Submit Selected" button in the Report view toolbar; partial submit support
- `submit/cross-view-sync`: `ItemsSubmitted` event published on MBassador event bus after every successful submit; both controllers subscribe and refresh

### Modified Capabilities

None

## Impact

- **Code**: None (documentation only of existing functionality)
- **Dependencies**: None
- **Configuration**: YAML `submit:` block already supported in config model
- **Tests**: Existing tests for `SubmitStatusTracker` (14), `SubmitSelectionManager` (10), `JsonSubmitConnector` (11) — these become the verification baseline