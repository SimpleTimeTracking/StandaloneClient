## Purpose

Manages per-connector checkbox selection state in memory, so users can select items for submission independently per connector without their choices being lost when switching connectors.

## ADDED Requirements

### Requirement: Per-Connector Selection Sets

The system SHALL maintain selection state keyed by connector ID, using a `Map<ConnectorId, Set<ItemKey>>` structure.

#### Scenario: Select item for connector
- **WHEN** a user checks an item for a specific connector
- **THEN** the item is added to that connector's selection set

#### Scenario: Deselect item for connector
- **WHEN** a user unchecks an item for a specific connector
- **THEN** the item is removed from that connector's selection set

#### Scenario: Selection isolated by connector
- **WHEN** a user selects items for connector A and switches to connector B
- **THEN** the selections for connector A are preserved and connector B starts with an empty set

### Requirement: Content-Based Item Keying

The system SHALL use the same content-based key scheme (`Base64(activity|start|end)`) as `SubmitStatusTracker` to ensure selection state is consistent across views.

#### Scenario: Same item key in selection and status
- **WHEN** an item is selected and checked for submission status
- **THEN** both systems compute the same key for the same item

### Requirement: Clear Selection

The system SHALL clear all selections for a given connector when submission completes.

#### Scenario: Clear after submit
- **WHEN** items are submitted successfully
- **THEN** the selection set for that connector is cleared