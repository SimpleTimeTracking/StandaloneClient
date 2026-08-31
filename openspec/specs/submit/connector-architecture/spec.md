# Connector Architecture Specification

## Purpose

Provides a pluggable connector interface for submitting time tracking entries to external systems, configured via YAML and wired through Dagger dependency injection.

## Requirements

### Requirement: Pluggable Connector Interface

The system SHALL define a `SubmitConnector` interface with an `id` property and two submission methods: `submitItems` for raw `TimeTrackingItem` objects and `submitSummary` for aggregated report items.

#### Scenario: Connector interface defines id and submit methods
- **WHEN** a new connector implementation is created
- **THEN** it MUST provide a unique `id` string, implement `submitItems(items: List<TimeTrackingItem>)`, and implement `submitSummary(report: Report, selectedItems: List<ReportListItem>)`

### Requirement: Connector Discovery via Dagger @IntoSet

The system SHALL discover available connectors via Dagger `@IntoSet` multibindings, allowing multiple connectors to be injected as a `Set<SubmitConnector>`.

#### Scenario: Multiple connectors discovered
- **WHEN** the Dagger component resolves `Set<SubmitConnector>`
- **THEN** all `@IntoSet`-annotated provider methods across all modules are included in the set

### Requirement: YAML Config for Connectors

Connectors MAY be configured under a `submit:` key in the YAML configuration file. Each connector entry specifies a `type` and a `file` path.

#### Scenario: Connector configured in YAML
- **WHEN** the YAML config contains `submit.connectors[0].type = "json"` and `submit.connectors[0].file = ".stt/submit.json"`
- **THEN** the system resolves the config and passes a `ConnectorConfig` to the matching connector's constructor

#### Scenario: Default config fallback
- **WHEN** no `submit:` section is present in the YAML config
- **THEN** the system provides a default `ConnectorConfig` with `type = "json"` and `file = ".stt/submit.json"`

### Requirement: ConnectorConfig Model

The system SHALL provide a `ConnectorConfig` data class with `type` and `file` string fields, and a `SubmitConfig` model holding a list of `ConnectorConfig` entries.

#### Scenario: Config model serialization
- **WHEN** the YAML is parsed into `ConfigRoot`
- **THEN** `ConfigRoot.submit` is a `SubmitConfig` containing the list of connector configurations