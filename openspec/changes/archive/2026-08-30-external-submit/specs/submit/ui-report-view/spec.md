## Purpose

Provides the Report view UI for submitting summarized time entries to external connectors, with a connector ComboBox, a three-state checkbox column, and a submit button supporting partial submission.

## ADDED Requirements

### Requirement: Connector Dropdown in Toolbar

The Report view SHALL display a ComboBox in its toolbar listing all available connectors, identical to the Activities view.

#### Scenario: Connector selection
- **WHEN** the user opens the connector dropdown
- **THEN** all registered `SubmitConnector` instances are listed by their `id`

### Requirement: Three-State Checkbox Column

Each row in the report table SHALL display a checkbox column with three states: all backing items submitted (checked, disabled), some backing items submitted (indeterminate, disabled), or no backing items submitted (unchecked, user-togglable).

#### Scenario: All items submitted - checkbox checked and disabled
- **WHEN** all backing items for a report row are submitted to the active connector
- **THEN** the checkbox is checked and disabled

#### Scenario: Some items submitted - checkbox indeterminate and disabled
- **WHEN** some but not all backing items for a report row are submitted to the active connector
- **THEN** the checkbox is in indeterminate state and disabled

#### Scenario: No items submitted - checkbox unchecked and toggleable
- **WHEN** no backing items for a report row are submitted to the active connector
- **THEN** the checkbox is unchecked and the user can toggle it

### Requirement: Partial Submit for Summary Rows

The system SHALL support partial submission of summarized report rows: only the unsubmitted backing items are sent to the connector when submitting a row with mixed submission status.

#### Scenario: Submit only unsubmitted items
- **WHEN** a row with some submitted and some unsubmitted backing items is submitted
- **THEN** only the unsubmitted backing items are included in the submission

### Requirement: Submit Selected Button

The Report view toolbar SHALL include a "Submit Selected" button that calls `SubmitConnector.submitSummary()` with the checked report rows.

#### Scenario: Submit selected rows
- **WHEN** the user clicks "Submit Selected" with checked rows
- **THEN** the system calls `SubmitConnector.submitSummary()` with the report and selected rows, marks the backing items as submitted, and publishes an `ItemsSubmitted` event