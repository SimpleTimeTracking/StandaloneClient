## Purpose

Ensures that submitting items in one view immediately refreshes the other view's state, preventing stale checkbox states and enabling a consistent user experience across the Activities and Report views.

## ADDED Requirements

### Requirement: ItemsSubmitted Event

The system SHALL publish an `ItemsSubmitted` event on the MBassador event bus after every successful submit operation.

#### Scenario: Event published on submit
- **WHEN** items are submitted from either the Activities view or the Report view
- **THEN** an `ItemsSubmitted` event is published on the event bus

### Requirement: Activities View Refreshes on External Submit

The Activities view SHALL subscribe to `ItemsSubmitted` events and refresh its activity list when items are submitted from the Report view.

#### Scenario: Activities view refreshes
- **WHEN** an `ItemsSubmitted` event is published from the Report view
- **THEN** the Activities view calls `activityList.refresh()` to recompute cell checkbox states

### Requirement: Report View Refreshes on External Submit

The Report view SHALL subscribe to `ItemsSubmitted` events and refresh its report table when items are submitted from the Activities view.

#### Scenario: Report view refreshes
- **WHEN** an `ItemsSubmitted` event is published from the Activities view
- **THEN** the Report view calls `tableForReport.refresh()` to recompute column checkbox states