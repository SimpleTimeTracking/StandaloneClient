# Settings View Delta Spec

## Purpose

Defines which configuration properties are editable in the settings tab of the main window, and how secret settings such as API tokens are edited and persisted. It guarantees that every user-facing configuration section (including `tempo:`) can be maintained without hand-editing the config file.

## ADDED Requirements

### Requirement: Settings Tab Exposes All Configuration Sections

The settings tab SHALL expose the properties of all user-facing configuration containers, including the Tempo configuration (`tempoToken`, `workdayStart`, `summaryMode`), in the property sheet, in addition to the already exposed sections (activities, backup, CLI, common prefix grouping, report, worktime, Jira).

#### Scenario: Tempo properties are listed

- **WHEN** the user opens the settings tab
- **THEN** the property sheet lists the Tempo properties `tempoToken`, `workdayStart`, and `summaryMode` with human-readable names

#### Scenario: Tempo values reflect current configuration

- **WHEN** the config file contains a non-default `workdayStart`, `summaryMode`, or a stored `tempoToken`
- **THEN** the corresponding editors in the settings tab show the currently configured values

#### Scenario: Edited tempo values are persisted

- **WHEN** the user changes a Tempo property in the settings tab and closes the application
- **THEN** the new value is written to the config file and restored on the next start

### Requirement: Secret Settings Are Edited With Masked Input

The settings tab SHALL edit secret settings (`PasswordSetting` values such as `tempoToken` and the Jira token) with a masked password field, and SHALL NOT display the stored secret as plain text on disk.

#### Scenario: Token editor masks input

- **WHEN** the user edits `tempoToken` in the settings tab
- **THEN** the input is masked (password field) and any pre-existing stored value is shown decrypted only inside that masked field

#### Scenario: Token is stored encrypted

- **WHEN** the user enters or changes `tempoToken` via the settings tab and the configuration is saved
- **THEN** the token is stored using the same encrypted/encoded representation as other secrets (AES-obfuscated `PasswordSetting`, serialized with the `!encrypted` YAML tag / Base64 in JSON), not as plain text

#### Scenario: Token round-trips through config file

- **WHEN** a token entered in the settings tab is saved and the application is restarted
- **THEN** the Tempo connector uses the saved token without re-entry, and the connector behaves as if the token had been configured by hand

#### Scenario: Token can be cleared

- **WHEN** the user empties the `tempoToken` field and the configuration is saved
- **THEN** no `tempoToken` remains configured and the Tempo connector reports that it is not configured
