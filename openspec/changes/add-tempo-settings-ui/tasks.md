## 1. Implementation

- [ ] 1.1 Inject `TempoConfig` into `SettingsController` constructor and add it to the bean list in `initialize()`; verify compilation (`./gradlew compileKotlin`) and that the GUI still starts (`./gradlew run`, manual)
- [ ] 1.2 Verify in the settings tab that `tempoToken`, `workdayStart`, and `summaryMode` appear with the expected editors (masked password field for the token); verify manually via `./gradlew run`

## 2. Tests

- [ ] 2.1 Extend the headless `SettingsController` test (or add one following `TestFX.installTK()` pattern) asserting that the property sheet contains the three Tempo properties and that the `tempoToken` item uses the `PasswordSetting` editor path; verify with `./gradlew test`
- [ ] 2.2 Add a round-trip test: enter a token via the `PasswordSetting` editor on `TempoConfig`, save via `YamlConfigService`/`JsonConfigService`, reload, and assert the decrypted token matches and the YAML/JSON contains no plain-text token; verify with `./gradlew test`

## 3. Verification

- [ ] 3.1 Run full build and test suite (`./gradlew build`) and confirm all tests pass on the current platform
- [ ] 3.2 Manually confirm end-to-end behavior: set `tempoToken` in the UI, restart, and check `~/.stt/stt.yaml` stores it as `!encrypted` and the Tempo connector accepts the configured token (completes the config-side counterpart of task 7.1 in `add-tempo-submit-connector`)
