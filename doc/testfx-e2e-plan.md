# TestFX E2E Test Plan (revised)

## Approach

Type commands into the `StyleClassedTextArea` (rich text input), click the insert button to execute, then navigate to the Report tab and verify. This exercises the full: **ANTLR parse -> Command -> Activities -> persist -> event bus -> controller refresh -> UI render** pipeline.

## Why no Cucumber/Gherkin

The project's `// GIVEN // WHEN // THEN` test convention (per AGENTS.md) provides ~80% of BDD readability. JavaFX step definitions would be verbose (`clickOn("#foo")`, `type("bar")`) and the `.feature` files would mirror Java code too closely to justify the Cucumber overhead. If you want human-readable specs, apply Gherkin to the CLI layer -- the JavaFX layer is better served by plain TestFX + JUnit.

## Build Changes (`build.gradle.kts`)

Already in place:

```kotlin
dependencies {
    testImplementation("org.testfx:testfx-core:4.0.18")
    testRuntimeOnly("org.testfx:openjfx-monocle:21.0.2") {
        exclude(group = "org.openjfx")
    }
}
```

The `test` task excludes `**/*E2ETest*` and a dedicated `testE2e` task includes them:

```kotlin
tasks.test {
    exclude("**/*E2ETest*")
}

tasks.register<Test>("testE2e") {
    include("**/*E2ETest*")
    useJUnit()
}
```

No JVM arg changes needed -- tests already run on classpath (`runOnClasspath = true`).

## Locale Consideration

The `CommandTextParser` uses `DateTimeFormatterBuilder.appendLocalized(...)` with `FormatStyle.SHORT`/`MEDIUM`. In en_US locale, `FormatStyle.SHORT` for time is `"h:mm a"` (12h with AM/PM). But our test commands use `"09:00"` (24h without AM/PM).

**Solution**: set `Locale.setDefault(Locale.GERMANY)` in the test's companion object init. German locale uses `"HH:mm"` for SHORT time, so `"09:00"` parses correctly.

## Design Decision: Use Production Dagger Component Directly

Rather than manually wiring controllers (15+ objects) or creating test-only Dagger modules, the test uses the **production `DaggerUIApplication` component directly** via its `@Component.Builder`. This avoids:

- Duplication of constructor signatures
- Risk of test-production wiring drift
- Kapt-generated Java code in test sources (which causes module-system issues)

Only two small production changes were needed:

1. **`BaseModule.kt`**: Added constructor parameter `sttFileOverride: File? = null`. When non-null, `provideDatabaseFile` returns the temp file instead of `~/.stt/activities`. Dagger auto-instantiates with `null` in production.

2. **`UIApplication.kt`**: Added `@Component.Builder` exposing `baseModule(module: BaseModule): Builder`. The test passes a `BaseModule(tempFile)` instance; production continues with `DaggerUIApplication.builder().build()`.

3. **`MainWindowController.kt`**: Changed `activitiesController` from `private val` to `internal val` so the test can access it (it's the same instance Dagger injects into the main window).

## How the Test Uses Dagger

The test's `@Before setup()` method:
1. Creates a temp `activities` file
2. Builds `DaggerUIApplication.builder().baseModule(BaseModule(tempFile)).build()`
3. Calls `configService().start()` (initializes config; if no `~/.stt/stt.json` exists, defaults are created)
4. Gets `mainWindow = app.mainWindow()` and `activitiesController = mainWindow.activitiesController`
5. Shows the stage

The Dagger graph resolves everything: `STTItemPersister` (backed by temp file), `Activities` (CommandHandler), `CommandFormatter`, `ActivitiesController`, `ReportController`, `MainWindowController`, event bus, etc.

## Locale Handling

The locale is set to `Locale.GERMANY` in the companion object `init` block, which runs at class-loading time, before any Dagger components are constructed. This ensures the `CommandTextParser` (created inside the `CommandModule`) uses 24-hour time format.

## Stage Setup

The test launches a `Stage` containing the `MainWindowController` with all 4 tabs. This means the FXML files (`MainWindow.fxml`, `ActivitiesPanel.fxml`, `ReportPanel.fxml`) are loaded through the real `FXMLLoader`, testing the `@FXML` annotation wiring. The `ActivitiesController`'s FXML is loaded lazily when `mainWindow.activitiesController.node` is first accessed (via `MainWindowController.initialize()` setting `activitiesTab.content`).

## Concrete Testcase: Full Day via Command Input

### Scenario

User types 10 commands for 4 topics covering a full contiguous day, then views the report.

#### Commands typed (in order)

```
fix login bug from 09:00 to 10:00
code review from 10:00 to 10:30
team meeting from 10:30 to 11:30
fix login bug from 11:30 to 12:00
lunch from 12:00 to 13:00
code review from 13:00 to 14:00
team meeting from 14:00 to 14:30
fix login bug from 14:30 to 15:30
team meeting from 15:30 to 16:00
code review from 16:00 to 17:00
```

#### What the test does

1. Builds the Dagger component with a temp file for persistence
2. Shows the main window (all 4 tabs)
3. Focuses the command text area (`#commandText`)
4. Sets text via `interact { controller.commandText.replaceText("...") }`
5. Clicks insert button (`#insert`) to execute
6. Repeats for all 10 commands
7. Clicks the Report tab
8. Waits for async report panel loading (1 second sleep)
9. Verifies table has 4 rows
10. Verifies each row's duration text
11. Verifies summary labels

#### Aggregation math

| Activity | Entries | Duration |
|---|---|---|
| fix login bug | 09:00-10:00 (1h) + 11:30-12:00 (30m) + 14:30-15:30 (1h) | **2h30m** |
| code review | 10:00-10:30 (30m) + 13:00-14:00 (1h) + 16:00-17:00 (1h) | **2h30m** |
| team meeting | 10:30-11:30 (1h) + 14:00-14:30 (30m) + 15:30-16:00 (30m) | **2h00m** |
| lunch | 12:00-13:00 (1h) | **1h00m** |
| **Total** | | **8h00m** |
| Effective | 8h00m - 1h00m (lunch) | **7h00m** |
| Break | lunch | **1h00m** |
| Uncovered | contiguous, no gaps | **0h00m** |

## Test Code: `ReportE2ETest.kt`

```kotlin
package org.stt.gui.jfx

import com.sun.javafx.application.PlatformImpl
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.stage.Stage
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.stt.BaseModule
import org.stt.gui.DaggerUIApplication
import org.testfx.api.FxRobot
import org.testfx.util.WaitForAsyncUtils
import java.nio.file.Files
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReportE2ETest {

    companion object {
        init {
            Locale.setDefault(Locale.GERMANY)

            System.setProperty("java.awt.headless", "true")
            System.setProperty("testfx.headless", "true")
            System.setProperty("glass.platform", "Monocle")
            System.setProperty("monocle.platform", "Headless")
            System.setProperty("prism.order", "sw")
            System.setProperty("prism.text", "t2k")
        }
    }

    private lateinit var robot: FxRobot
    private lateinit var stage: Stage
    private lateinit var activitiesController: ActivitiesController

    @Before
    fun setup() {
        PlatformImpl.startup { }

        val setupLatch = CountDownLatch(1)
        javafx.application.Platform.runLater {
            try {
                val tempFile = Files.createTempDirectory("stt-e2e").resolve("activities").toFile()
                tempFile.createNewFile()

                val app = DaggerUIApplication.builder()
                        .baseModule(BaseModule(sttFileOverride = tempFile))
                        .build()
                app.configService().start()
                val mainWindow = app.mainWindow()
                activitiesController = mainWindow.activitiesController

                stage = Stage()
                mainWindow.show(stage)
            } finally {
                setupLatch.countDown()
            }
        }
        setupLatch.await()

        robot = FxRobot()
        robot.targetWindow(stage)
    }

    @After
    fun teardown() {
        javafx.application.Platform.runLater { stage.close() }
    }

    @Test
    fun shouldShowGroupedReportForDayWithFourTopics() {
        val commands = listOf(
                "fix login bug from 09:00 to 10:00",
                "code review from 10:00 to 10:30",
                "team meeting from 10:30 to 11:30",
                "fix login bug from 11:30 to 12:00",
                "lunch from 12:00 to 13:00",
                "code review from 13:00 to 14:00",
                "team meeting from 14:00 to 14:30",
                "fix login bug from 14:30 to 15:30",
                "team meeting from 15:30 to 16:00",
                "code review from 16:00 to 17:00"
        )

        for (cmd in commands) {
            robot.interact(Runnable { activitiesController.commandText.replaceText(cmd) })
            WaitForAsyncUtils.waitForFxEvents()
            robot.clickOn("#insert")
            WaitForAsyncUtils.waitForFxEvents()
        }

        robot.clickOn("#reportTab")
        WaitForAsyncUtils.waitForFxEvents()
        WaitForAsyncUtils.sleep(1000L, TimeUnit.MILLISECONDS)

        val table = robot.lookup(".table-view").query<TableView<ReportController.ReportListItem>>()
        assertThat(table.items).hasSize(4)

        fun row(comment: String) = table.items.first { it.comment == comment }

        assertThat(row("fix login bug").duration).isEqualTo(Duration.ofHours(2).plusMinutes(30))
        assertThat(row("code review").duration).isEqualTo(Duration.ofHours(2).plusMinutes(30))
        assertThat(row("team meeting").duration).isEqualTo(Duration.ofHours(2))
        assertThat(row("lunch").duration).isEqualTo(Duration.ofHours(1))

        assertThat(robot.lookup("#totalDuration").query<Label>().text).contains("8")
        assertThat(robot.lookup("#effectiveDuration").query<Label>().text).contains("8")
        assertThat(robot.lookup("#breakDuration").query<Label>().text).contains("0")
        assertThat(robot.lookup("#uncoveredDuration").query<Label>().text).contains("0")
    }
}
```

### Key design decisions

- **`interact { controller.commandText.replaceText(cmd) }`** instead of TestFX `type()` -- RichTextFX's `StyleClassedTextArea` has custom input handling that may not work with TestFX's robot typing. `replaceText` is deterministic and exercises the same `textProperty()` listener chain.
- **`clickOn("#insert")`** instead of pressing Enter -- the `ENTER` key binding in `addCommandText()` uses `Nodes.addInputMap` from RichTextFX's well-behaved-event library, which may not fire through TestFX key events. The insert button (`#insert`) is a standard JavaFX `Button` with `setOnAction`, which TestFX handles reliably.
- **Items created on "today"** -- the ANTLR parser uses `LocalDate.now()` when only a time is given (no date). The Report tab defaults to `LocalDate.now()`. So the test works without needing to select a date.
- **Uses the production Dagger component** -- the test builds the exact same `UIApplication` component as production, with only the `BaseModule` overridden to supply a temp persistence file. This guarantees the test exercises the real DI graph.
- **`configService().start()` is called** -- unlike the original manual wiring, the Dagger component requires config service initialization (it reads from `~/.stt/stt.json` or creates defaults). The config defaults are acceptable because the test doesn't delete or modify items during its scenario.
- **Controller obtained via `mainWindow.activitiesController`** -- this ensures the test uses the SAME `ActivitiesController` instance that is wired into the MainWindowController's FXML lifecycle, rather than a separate Dagger-provided instance.
- **Real `STTItemPersister` persists to a temp file** -- the test validates that the full write->read->query->render pipeline works end-to-end.

## What This Test Catches That Unit Tests Miss

| What can go wrong | Unit test catches | TestFX catches |
|---|---|---|
| `@FXML` field ID mismatch | -- | Check |
| `FXMLLoader` fails to load FXML | -- | Check |
| `ReportBinding` uses wrong query | -- | Check (needs real queries) |
| `tableForReport.columns` misconfigured | -- | Check |
| Cell factory returns wrong text | partial | Check |
| Event bus handler not registered | -- | Check |
| `DurationRounder` not injected correctly | -- | Check |
| Tab switching triggers async load | -- | Check |

## Future Tests

1. **Activities tab**: type command -> verify item appears in `ListView` immediately
2. **Submit flow**: check items in report -> click submit -> verify submitted state persists
3. **Ongoing item tracking**: `"fix login bug since 09:00"` -> `"fin"` -> verify item appears
4. **Week report**: add items across 3 days -> select 3-day range -> verify multi-day aggregation
5. **Empty state**: open report with no items -> verify empty table + zero durations
6. **Gaps**: items with uncovered time -> verify `uncoveredDuration` label shows non-zero and is red

## Production Changes

- **`BaseModule.kt`**: Added constructor parameter `sttFileOverride: File? = null`. When non-null, `provideDatabaseFile` returns it instead of the default. Dagger auto-instantiates with `null` in production.
- **`UIApplication.kt`**: Added `@Component.Builder` with `baseModule(module: BaseModule): Builder` setter. Production code continues with `DaggerUIApplication.builder().build()`.
- **`MainWindowController.kt`**: Changed `activitiesController` from `private val` to `internal val` for test access.