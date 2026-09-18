## ADDED Requirements

### Requirement: Submit Result Notification with Owner Window

The system SHALL show the user a notification with the submit result after every submit operation. The notification SHALL be anchored to an owner window so it reliably appears on all supported platforms, including macOS.

#### Scenario: Success summary shown after submit
- **WHEN** a submit to a connector succeeds (fully or partially)
- **THEN** a notification with a summary of the result (submitted, skipped, failed counts) is shown to the user

#### Scenario: Failure feedback shown after submit
- **WHEN** a submit operation fails
- **THEN** a notification with a failure message is shown to the user

#### Scenario: Notification anchored to owner window
- **WHEN** any submit result notification is shown
- **THEN** it is displayed anchored to the application's main window rather than without an owner

#### Scenario: Notification shown when submitting from either view
- **WHEN** a submit is initiated from the Activities view or the Report view
- **THEN** the submit result notification is shown in both cases

#### Scenario: Long messages stay within the window
- **WHEN** a submit result notification with a message longer than the typical summary is shown
- **THEN** the notification stays within the bounds of the owner window and does not extend beyond it to the right
