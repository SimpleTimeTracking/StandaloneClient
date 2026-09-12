# Status Tracking Specification

## Purpose

Tracks which items have been submitted to which connectors, with persistent state that survives application restarts, and prevents re-submission of already-submitted items.

## Requirements

### Requirement: Per-Item Per-Connector Status Tracking

The system SHALL track submission status for each item individually per connector, identifying items by their content (activity, start time, end time) rather than by identity.

#### Scenario: Mark item as submitted
- **WHEN** an item is submitted to a connector
- **THEN** the system marks it as submitted for that connector ID with the current timestamp

#### Scenario: Check submission status for any connector
- **WHEN** checking if an item is submitted to any connector
- **THEN** the system returns `true` if the item exists in any connector's submission map

#### Scenario: Check submission status for a specific connector
- **WHEN** checking if an item is submitted to a specific connector
- **THEN** the system returns `true` only if the item is marked for that connector ID

### Requirement: Content-Based Item Keying

The system SHALL compute a content-based key for each item using the formula `Base64(activity|start|end)` to ensure the same logical item is recognized across application restarts.

#### Scenario: Same content recognized across restarts
- **WHEN** an item with the same activity, start, and end values is loaded after a restart
- **THEN** the system recognizes it as the same item for submission status purposes

#### Scenario: Item without end is tracked
- **WHEN** an item has no end time (ongoing)
- **THEN** the system computes a key using `activity|start|null` and tracks it correctly

### Requirement: Persistence Across Restarts

The system SHALL persist submission status to `~/.stt/submit-status` as pipe-delimited lines (`itemKey|connectorId|ISO-8601-timestamp`) and reload it on startup.

#### Scenario: Status persists between sessions
- **WHEN** the application shuts down and restarts
- **THEN** all previously submitted items are still marked as submitted

#### Scenario: Status file is created on first submit
- **WHEN** the first item is submitted
- **THEN** the `~/.stt/` directory and `submit-status` file are created if they do not exist

### Requirement: Submit Fraction Calculation

The system SHALL provide a method to calculate the fraction of items in a list that have been submitted to a given connector, used for partial-submit indicators in the Report view.

#### Scenario: All items submitted
- **WHEN** all items in a list are submitted to the connector
- **THEN** the fraction is 1.0

#### Scenario: No items submitted
- **WHEN** no items in a list are submitted to the connector
- **THEN** the fraction is 0.0

#### Scenario: Some items submitted
- **WHEN** some but not all items in a list are submitted to the connector
- **THEN** the fraction is `submittedCount / totalCount`

#### Scenario: Empty list
- **WHEN** the list is empty
- **THEN** the fraction is 1.0