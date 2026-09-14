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
