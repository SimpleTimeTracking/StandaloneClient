# Status Tracking Delta Spec

## MODIFIED Requirements

### Requirement: Persistence Across Restarts

The system SHALL persist submission status to `~/.stt/submit-status` as pipe-delimited lines (`itemKey|connectorId|ISO-8601-timestamp`), reload it on startup, and write each mark to disk immediately when the mark is made (not only at shutdown), so a crash after successful submission cannot lose the mark.

#### Scenario: Status persists between sessions
- **WHEN** the application shuts down and restarts
- **THEN** all previously submitted items are still marked as submitted

#### Scenario: Status file is created on first submit
- **WHEN** the first item is submitted
- **THEN** the `~/.stt/` directory and `submit-status` file are created if they do not exist

#### Scenario: Mark is persisted immediately
- **WHEN** an item is marked as submitted and the application crashes before shutdown
- **THEN** the mark is present in the status file and the item is still marked as submitted on the next start

## ADDED Requirements

### Requirement: Connectors Own Submission Marking

The system SHALL have each connector mark the items it successfully handled as submitted for its own connector ID, and views SHALL NOT mark items themselves. Items that a connector skipped or failed to submit SHALL remain unmarked and submittable.

#### Scenario: Connector marks successfully handled items
- **WHEN** a connector submission run completes
- **THEN** only the items the connector successfully submitted are marked as submitted for that connector's ID

#### Scenario: Failed or skipped items remain unmarked
- **WHEN** a connector submission run contains items that could not be submitted or were deliberately skipped
- **THEN** those items are not marked as submitted and remain submittable in a later run

#### Scenario: Views do not mark items
- **WHEN** a view triggers a submission through a connector
- **THEN** the view does not mark any item as submitted itself; marking is done exclusively by the connector
