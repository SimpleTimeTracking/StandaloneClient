# AGENTS.md - SimpleTimeTracking (STT)

## Purpose

SimpleTimeTracking (STT) is a cross-platform desktop application for **fast, unobtrusive time tracking**. 
It prioritizes the individual user's workflow over management reporting — design goal is to let you start/stop tracking with minimal friction and copy results into other systems.

## Use Cases

- **Start/stop working** on a task with a single command or button click
- **Resume** the last or any previous activity
- **Search** across historical activities by comment text
- **Report** on time spent per activity/group (day, period, search filter)
- **Overtime** calculation against configured worktime rules
- **CSV/Jira export** for integration with other systems
- **Dual interface**: JavaFX GUI for desktop use, CLI for scripting/terminal use
- **Automatic backup** and item logging

## Architecture

### Tech Stack

| Layer | Technology |
|---|---|
| Language | **Kotlin** (JVM), with some Java source files |
| UI | **JavaFX 21**, ControlsFX, RichTextFX |
| Build | **Gradle** (Kotlin DSL), JDK 21+ |
| DI | **Dagger 2** (annotation-based, `kapt`) |
| Parsing | **ANTLR 4** for command text parsing |
| Config | **YAML** (SnakeYAML) |
| Event Bus | **MBassador** (in-process pub/sub) |
| Testing | **JUnit 4**, **AssertJ**, **Mockito** (+ mockito-kotlin), TestFX |

### Module / Package Layout

All source under `src/main/kotlin/org/stt/`:

```
org.stt/
├── cli/           # CLI entry points (Main, ReportPrinter, FormatConverter)
├── command/       # Command pattern: Commands.kt, CommandHandler.kt, CommandFormatter (ANTLR)
├── config/        # Configuration loading from YAML, config classes
├── connector/jira/ # Jira integration (REST client)
├── csv/           # CSV import/export
├── event/         # Event bus classes (NotifyUser, ShuttingDown, TimePassedEvent)
├── gui/           # JavaFX UI (MainWindow, ActivitiesController, ReportController, Settings)
├── model/         # Domain model (TimeTrackingItem, ReportingItem)
├── persistence/   # ItemReader / ItemWriter interfaces + STT file format implementation
├── query/         # Query/filter logic over time tracking items (Criteria, WorkTimeQueries)
├── reporting/     # Report generation (SummingReportGenerator, OvertimeReportGenerator)
├── text/          # Text completion, categorization, grouping (CommonPrefixGrouper, JiraExpansion)
├── time/          # Date/time utilities, duration rounding
├── update/        # Update check mechanism
├── validation/    # Input validation
```

> See [doc/ui-architecture.md](doc/ui-architecture.md) for a detailed breakdown of UI components, controllers, data objects, and event bus wiring.

### Key Design Patterns

- **Command pattern**: `Command` (sealed hierarchy) + `CommandHandler` interface (Visitor), parsed via ANTLR grammar
- **Dependency Injection**: Dagger `@Module` / `@Provides` / `@Inject`; components are `Dagger*Application` (e.g., `DaggerCLIApplication`, `DaggerUIApplication`)
- **Event-driven**: `MBassador` event bus for decoupled UI updates (time ticks, shutdown, notifications)
- **Repository**: `ItemReader` / `ItemWriter` interfaces abstract storage; `STTItemReader` / `STTItemWriter` implement the plain-text file format
- **Service lifecycle**: `Service` interface with `start()`/`stop()` for config, backup, and logging services
- **Data stored**: A plain-text file (`.stt/activities`) with one record per line

## Code Style

- **Language**: Kotlin (prefer immutable data classes, `val`, extension functions)
- **Naming**: `camelCase` for methods/variables, `PascalCase` for classes, no underscores
- **Test naming**: `should[Expectation]` — e.g., `shouldCreateItemWithoutEnd`
- **Test structure**: GIVEN / WHEN / THEN comment annotations, `sut` (system under test) variable
- **Imports**: explicit single imports (no wildcard `.*` except for standard lib / assertions)
- **Nullability**: explicit nullable types with `?`, prefer `?:` elvis operator
- **DI**: constructor injection via `@Inject`, module-provided bindings for platform/third-party types
- **Logging**: `java.util.logging.Logger` (`Logger.getLogger(...)`)
- **File format**: one time-tracking record per line, human-readable text

## Build & Test

```bash
./gradlew build          # compile + test + assemble fat jar
./gradlew test           # run all tests (JUnit 4)
./gradlew check          # + static analysis (SonarQube if configured)
./gradlew dist           # jlink + jpackage for native distribution
./gradlew run            # compile and start the GUI application
```

Output fat jar: `build/libs/STT-<version>.jar`

## Commit Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add resume-last-activity CLI command
fix: report crashes on empty activity list
refactor: extract DurationRounder from ReportGenerator
test: add overtime calculation edge cases
docs: update README with new CLI usage
chore: bump Dagger to 2.50
```

Scopes (optional): `cli`, `gui`, `persistence`, `query`, `reporting`, `config`, `time`, `deps`