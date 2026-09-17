## Context

The settings tab is a ControlsFX `PropertySheet` built programmatically in `SettingsController` (no FXML). It enumerates bean properties of injected `ConfigurationContainer` singletons via `BeanPropertyUtils.getProperties`, filtering out properties typed as `ConfigurationContainer` (nested sections). Two custom editors exist, selected by property type in the editor factory:

- `PasswordSetting` → masked `PasswordField` editor: shows the decrypted value on load, re-wraps edits via `PasswordSetting.fromPassword(...)` (AES-obfuscated in memory).
- `PathSetting` → text field editor.

`TempoConfig` (tempoToken, workdayStart, summaryMode) exists and is provided by `ConfigModule`, but is not injected into `SettingsController`, so its properties never reach the property sheet. `tempoToken` has the same nullable `PasswordSetting?` shape as `jiraToken` in `JiraConfig`. Persistence is centralized: editors mutate the singleton config beans directly; `ConfigServiceFacade.stop()` → `YamlConfigService`/`JsonConfigService` writes everything at shutdown, already handling `PasswordSetting` with the `!encrypted` YAML tag / Base64 JSON encoding.

## Goals / Non-Goals

**Goals:**

- All three Tempo properties editable in the settings tab.
- `tempoToken` edited and stored exactly like the Jira token (masked input, `PasswordSetting`, `!encrypted`/Base64 serialization).
- Zero changes to config format, `PasswordSetting`, `YamlConfigService`, `JsonConfigService`, or the Tempo connector.

**Non-Goals:**

- No explicit Save/Apply button (existing behavior: values apply immediately to the shared beans, persisted at shutdown).
- No restructure of the property sheet (grouping, search, categories) for all sections.
- No new Tempo settings (URL, user) — the connector hard-codes the Tempo Cloud base URL by design (see `add-tempo-submit-connector/design.md`).

## Decisions

### D1: Reuse the existing `PropertySheet` + bean-enumeration mechanism

Add `TempoConfig` to the `SettingsController` constructor and to the `Stream.of(...)` bean list in `initialize()`. `ConfigModule` already provides `TempoConfig`, so Dagger wiring is a one-line constructor change — no module edits.

*Alternative considered:* a dedicated FXML form for Tempo settings. Rejected: inconsistent with the existing settings UI, more code, and the `PropertySheet` already has editors for every needed type.

### D2: No new editor — `tempoToken` rides the existing `PasswordSetting` editor

The editor factory dispatches on property type (`PasswordSetting::class.java.isAssignableFrom(type)`), independent of which bean a property belongs to. Once `tempoToken` appears in the property sheet it automatically gets the masked editor, decrypted display, and re-encryption on edit. Serialization to `!encrypted`/Base64 happens unchanged at shutdown.

*Alternative considered:* a separate Tempo-specific token editor. Rejected: duplicates `createPasswordSettingsEditor` for no behavioral difference.

### D3: Default editors for `workdayStart` and `summaryMode`

`workdayStart` (Duration) and `summaryMode` (enum) fall through to the ControlsFX default editor factory, same as comparable properties in other containers. If the default enum editor proves unusable in practice (headless test or manual check), a simple `ComboBox`-based editor for `TempoSummaryMode` is a contained follow-up that does not affect the spec.

## Risks / Trade-offs

- [Editors write directly into shared singletons; a change is live for the running app even if the app crashes before shutdown] → Same behavior as all existing settings; no change.
- [ControlsFX `BeanPropertyUtils` may name/skip properties differently for nullable `PasswordSetting?`] → `jiraToken` is also `PasswordSetting?` and already works; the Tempo properties will be verified in the headless `SettingsController` test.
- [Displaying the decrypted token in a masked field means it is momentarily in memory as plain text] → Identical to the Jira token editor; the token stays out of the config file as plain text.

## Migration Plan

None required — no config format or persisted-data change. Rollback is a revert of the controller change.
