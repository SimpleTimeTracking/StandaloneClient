## Why

The Tempo submit connector (change `add-tempo-submit-connector`) requires a `tempo:` config section (`tempoToken`, `workdayStart`, `summaryMode`), but the settings tab does not expose it. Users currently have to hand-edit `~/.stt/stt.yaml`, and there is no way to enter or change the Tempo token from the UI. The settings UI should cover all Tempo-related properties, with the token edited like the Jira token (masked input, `PasswordSetting` encryption with the `!encrypted` YAML tag).

## What Changes

- Inject `TempoConfig` into `SettingsController` and add its properties to the settings `PropertySheet`, alongside the existing config containers.
- `tempoToken` is edited with the existing masked `PasswordField` editor and stored as a `PasswordSetting` (AES-obfuscated, serialized with the `!encrypted` YAML tag / Base64 JSON encoding) — the same mechanism as the Jira token.
- `workdayStart` and `summaryMode` become editable with default ControlsFX editors (no new editor types required).
- No changes to the config file format, `TempoConfig`, `PasswordSetting`, or the Tempo connector itself; persistence keeps happening at shutdown via the existing `ConfigServiceFacade`.

## Capabilities

### New Capabilities

- `gui/settings-view`: The settings tab of the main window — which configuration properties are editable in the UI and how secret settings (tokens/passwords) are edited and stored.

### Modified Capabilities

<!-- none: no existing spec covers the settings tab; the Tempo connector spec (submit/tempo-connector) is unchanged because config format and connector behavior stay the same. -->

## Impact

- `src/main/kotlin/org/stt/gui/jfx/SettingsController.kt` — add `TempoConfig` to constructor and `PropertySheet` beans.
- Dagger wiring: `ConfigModule` already provides `TempoConfig`, so only the constructor injection changes; no new module needed.
- No new dependencies, no config format changes, no connector changes.
- Tests: extend/adjust `SettingsController` tests if present; otherwise cover via existing headless JavaFX test setup.
