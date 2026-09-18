## MODIFIED Requirements

### Requirement: Three-State Checkbox Column

Each row in the report table SHALL display a checkbox column with three states: all backing items submitted (checked, disabled), some backing items submitted (indeterminate, disabled), or no backing items submitted (unchecked, user-togglable). The checkbox state SHALL be recomputed against the active connector whenever the table is refreshed, including after an `ItemsSubmitted` event.

#### Scenario: All items submitted - checkbox checked and disabled
- **WHEN** all backing items for a report row are submitted to the active connector
- **THEN** the checkbox is checked and disabled

#### Scenario: Some items submitted - checkbox indeterminate and disabled
- **WHEN** some but not all backing items for a report row are submitted to the active connector
- **THEN** the checkbox is in indeterminate state and disabled

#### Scenario: No items submitted - checkbox unchecked and toggleable
- **WHEN** no backing items for a report row are submitted to the active connector
- **THEN** the checkbox is unchecked and the user can toggle it

#### Scenario: Checkbox state updated after in-session submit
- **WHEN** a submit succeeds and an `ItemsSubmitted` event is published
- **THEN** the affected rows' checkboxes immediately reflect the new submitted state without an application restart

#### Scenario: Lock icon beside the checkbox
- **WHEN** all backing items of a report row are submitted to any connector
- **THEN** a lock icon is shown next to the row's submit checkbox, matching the indicator used in the Activities view

## ADDED Requirements

### Requirement: Row-Level Submitted Indication

The Report view SHALL visually indicate that a report row is immutable: when all backing items of a row are submitted to any connector, a dedicated lock icon SHALL be shown to the left of the row's submit checkbox. The activity text SHALL NOT carry an additional submitted indicator.

#### Scenario: Fully submitted row shows lock icon
- **WHEN** all backing items of a report row are submitted to any connector
- **THEN** a lock icon is shown to the left of the row's submit checkbox

#### Scenario: Row indication updates without restart
- **WHEN** all backing items of a row become submitted (via submit from either view)
- **THEN** the lock icon is shown on the row immediately, without an application restart

#### Scenario: Row indication after restart
- **WHEN** the application is restarted and a report is shown containing fully submitted rows
- **THEN** those rows display the lock icon

#### Scenario: Partially submitted row shows no lock icon
- **WHEN** only some backing items of a row are submitted
- **THEN** the row does not display a lock icon

#### Scenario: No duplicate indicator in the activity text
- **WHEN** a report row is fully submitted
- **THEN** the row's activity text is rendered without any additional submitted indicator

### Requirement: Last-Used Connector Preselected

The Report view SHALL preselect the persisted last-used connector in its connector ComboBox on startup, falling back to the first registered connector when no valid id is stored.

#### Scenario: Preselect last-used connector
- **WHEN** the Report view is created and a valid last-used connector id is configured
- **THEN** the connector ComboBox preselects that connector

#### Scenario: Fallback when stored id is unknown
- **WHEN** the stored connector id does not match any registered connector
- **THEN** the ComboBox preselects the first registered connector
