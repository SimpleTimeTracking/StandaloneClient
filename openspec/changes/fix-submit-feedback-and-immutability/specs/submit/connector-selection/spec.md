## Purpose

Persists the last-used submit connector so both the Activities view and the Report view default to the same, most recently used connector on startup, instead of always defaulting to the first connector in the list.

## ADDED Requirements

### Requirement: Persisted Last-Used Connector

The system SHALL persist the id of the connector the user last selected in a connector ComboBox to the application configuration, and SHALL restore it as the selected connector in both views on startup.

#### Scenario: Selection persisted when connector changes
- **WHEN** the user selects a connector in the ComboBox of either view
- **THEN** the connector's id is persisted to the configuration

#### Scenario: Last-used connector preselected on startup
- **WHEN** the application starts and a last-used connector id is configured
- **THEN** both views preselect the connector with that id

#### Scenario: Fallback when stored id is unknown
- **WHEN** the stored connector id does not match any registered connector (e.g. connector removed from configuration)
- **THEN** both views fall back to the first registered connector

#### Scenario: Fallback when nothing stored
- **WHEN** no last-used connector id is stored
- **THEN** both views fall back to the first registered connector

#### Scenario: Persistence survives restarts
- **WHEN** the user selects the Tempo connector, shuts down, and restarts the application
- **THEN** the Tempo connector is preselected in both views

#### Scenario: Persisted immediately on change
- **WHEN** the user selects a connector in either view
- **THEN** the selection is written to the configuration store immediately, so it survives even an abnormal termination

### Requirement: Selection Synchronized Across Views

The connector ComboBoxes of the Activities and Report views SHALL be synchronized: selecting a connector in one view selects the same connector in the other view.

#### Scenario: Change in one view propagates to the other
- **WHEN** the user selects a connector in one view
- **THEN** the other view's ComboBox selects the same connector

#### Scenario: No oscillation
- **WHEN** a selection change propagates to the other view
- **THEN** the propagation stops once both views show the same connector
