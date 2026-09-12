## Purpose

Provides the Activities view UI for selecting and submitting time tracking items to external connectors, with a connector ComboBox, per-cell checkboxes, and a submit button.

## ADDED Requirements

### Requirement: Connector Dropdown in Toolbar

The Activities view SHALL display a ComboBox in its toolbar listing all available connectors, allowing the user to select the active connector.

#### Scenario: Connector selection
- **WHEN** the user opens the connector dropdown
- **THEN** all registered `SubmitConnector` instances are listed by their `id`

#### Scenario: Checkbox state tied to active connector
- **WHEN** the user selects a different connector from the dropdown
- **THEN** all checkbox states are recomputed against the new connector

### Requirement: Per-Cell Submit Checkbox

Each `TimeTrackingItemCellWithActions` SHALL display a checkbox that, when checked, marks the item for submission to the active connector.

#### Scenario: Checkbox unchecked for unsubmitted item
- **WHEN** an item is not yet submitted to the active connector
- **THEN** the checkbox is unchecked and the user can toggle it

#### Scenario: Checkbox checked and disabled for submitted item
- **WHEN** an item is already submitted to the active connector
- **THEN** the checkbox is checked and disabled, and the user cannot toggle it

### Requirement: Submit Selected Button

The Activities view toolbar SHALL include a "Submit Selected" button that sends all checked items to the active connector.

#### Scenario: Submit selected items
- **WHEN** the user clicks "Submit Selected" with checked items
- **THEN** the system calls `SubmitConnector.submitItems()` with the checked items, marks them as submitted via `SubmitStatusTracker.markSubmitted()`, clears the selection, and publishes an `ItemsSubmitted` event

#### Scenario: Submit button disabled when no items checked
- **WHEN** no items are checked
- **THEN** the "Submit Selected" button is disabled