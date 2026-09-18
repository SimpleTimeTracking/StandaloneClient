## MODIFIED Requirements

### Requirement: Submit Selected Button

The Activities view toolbar SHALL include a "Submit Selected" button that sends all checked items to the active connector.

#### Scenario: Submit selected items
- **WHEN** the user clicks "Submit Selected" with checked items
- **THEN** the system resolves the checked items using the same connector id that the checkboxes stored selections under (`connector.id`), calls `SubmitConnector.submitItems()` with the checked items, marks them as submitted via `SubmitStatusTracker.markSubmitted()`, clears the selection, and publishes an `ItemsSubmitted` event

#### Scenario: Submit button disabled when no items checked
- **WHEN** no items are checked
- **THEN** the "Submit Selected" button is disabled

#### Scenario: Selection lookup uses connector id
- **WHEN** an item is checked while connector "tempo" is active and the user clicks "Submit Selected"
- **THEN** the item is found via the selection set stored under connector id `tempo` and included in the submission

## ADDED Requirements

### Requirement: Submitted Item Lock Icon

Each `TimeTrackingItemCellWithActions` SHALL display a dedicated lock icon for items that are submitted to any connector, matching the immutable indication used in the Report view.

#### Scenario: Submitted item shows lock icon
- **WHEN** an item is submitted to any connector
- **THEN** the item's cell displays a lock icon

#### Scenario: Unsubmitted item shows no lock icon
- **WHEN** an item is not submitted to any connector
- **THEN** the item's cell does not display a lock icon

#### Scenario: Lock icon updates without restart
- **WHEN** an item becomes submitted via a submit from either view
- **THEN** the lock icon is shown on the item's cell immediately

### Requirement: Last-Used Connector Preselected

The Activities view SHALL preselect the persisted last-used connector in its connector ComboBox on startup, falling back to the first registered connector when no valid id is stored.

#### Scenario: Preselect last-used connector
- **WHEN** the Activities view is created and a valid last-used connector id is configured
- **THEN** the connector ComboBox preselects that connector

#### Scenario: Fallback when stored id is unknown
- **WHEN** the stored connector id does not match any registered connector
- **THEN** the ComboBox preselects the first registered connector

### Requirement: Connector Selection Synchronized Across Views

The Activities view SHALL keep its connector selection in sync with the Report view: when the connector is changed in the Report view, the Activities view selects the same connector, and vice versa.

#### Scenario: Selection follows a change in the other view
- **WHEN** the user selects connector "tempo" in the Report view
- **THEN** the Activities view selects connector "tempo" as well

#### Scenario: Synchronized selection updates checkbox states
- **WHEN** the connector selection changes due to a change in the other view
- **THEN** the view's checkbox states are recomputed against the newly selected connector
