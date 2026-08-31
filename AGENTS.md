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
- **Imports**: explicit single imports (no wildcard `.*` except for standard lib / assertions)
- **Nullability**: explicit nullable types with `?`, prefer `?:` elvis operator
- **DI**: constructor injection via `@Inject`, module-provided bindings for platform/third-party types
- **Logging**: `java.util.logging.Logger` (`Logger.getLogger(...)`)
- **File format**: one time-tracking record per line, human-readable text

## Testing

### Framework & Dependencies

| Tool | Dependency | Purpose |
|------|-----------|---------|
| **JUnit 4** | `junit:junit-dep:4.11` | Runner (`@Test`, `@Before`, `@Theory`, `@RunWith(Theories::class)`, `@DataPoints`, `@TestedOn`, `@Rule`) |
| **AssertJ** | `assertj-core:3.26.3` | Fluent assertions (`assertThat(...)`) |
| **Mockito** | `mockito-core:5.12.0` | Mocking (`@Mock`, `MockitoAnnotations.initMocks()`, `given()`/`verify()`) |
| **Mockito Kotlin** | `mockito-kotlin:5.4.0` | Kotlin-friendly matchers (`any()`, `anyOrNull()`) |
| **TestFX** | via monocle | Headless JavaFX testing |
| **Commons IO** | `commons-io:2.8.0` | Temp file I/O in tests (`TemporaryFolder`) |

> Mockito inline mock maker is enabled via `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` (`mock-maker-inline`) for mocking final classes.

### Test Location & Layout

Tests mirror `src/main/kotlin/org/stt/` one-to-one under `src/test/kotlin/org/stt/`:

```
src/test/kotlin/org/stt/
├── cli/                      # MainTest, ReportPrinterTest
├── command/                  # ActivitiesTest, CommandFormatterTest
├── config/                   # PasswordSettingTest
├── connector/jira/           # JiraClientTest
├── csv/importer/             # CsvImporterTest
├── gui/jfx/                  # ActivitiesControllerTest, TimeTrackingItemCellTest
│   └── binding/              # ReportBindingTest
├── importer/                 # STTItemReaderTest, STTItemWriterTest, TiImporterTest
├── model/                    # TimeTrackingItemTest
├── persistence/              # BackupCreatorTest, IOUtilTest
│   └── stt/                  # STTItemConverterTest
├── query/                    # TimeTrackingItemQueriesTest
├── reporting/                # SummingReportGeneratorTest, OvertimeReportGeneratorTest, WorkingtimeItemProviderTest
├── text/                     # CommonPrefixGrouperTest, JiraExpansionProviderTest
├── time/                     # DateTimesTest, DurationRounderTest, IntervalTest
├── update/                   # VersionComparatorTest
├── validation/               # ItemAndDateValidatorTest
├── IntRangeTest.kt           # (root-level utility)
├── StatesTest.kt             # (root-level utility)
├── StringsTest.kt            # (root-level utility)
├── Tests.kt                  # Mockito matcher helpers (Matchers.argThat, Matchers.any)
└── ItemReaderTestHelper.kt   # Stubbing helper (givenReaderReturns)
```

### Naming Convention

```
should[Expectation]  →  e.g., shouldReturnTrueForSameDate, shouldCreateItemWithoutEnd
```

### Test Structure Pattern

Every test follows **GIVEN / WHEN / THEN** comments. The System Under Test is always named `sut`:

```kotlin
@Test
fun shouldDoSomething() {
    // GIVEN
    ...
    // WHEN
    val result = sut.method()
    // THEN
    assertThat(result).isEqualTo(...)
}
```

### Mock Setup Pattern

```kotlin
@Mock private lateinit var dependency: SomeClass
private lateinit var sut: ClassUnderTest

@Before
fun setup() {
    MockitoAnnotations.initMocks(this)
    sut = ClassUnderTest(dependency)
}
```

### Assertion Style

Pure AssertJ chaining — no JUnit `assertEquals`/`assertTrue`:

```kotlin
assertThat(result).isEqualTo(expected)
assertThat(result).isTrue()
assertThat(list).hasSize(3).containsExactly(a, b, c)
```

### Parameterized Tests

Use JUnit 4 `Theories` with `@DataPoints` or `@TestedOn` for data-driven tests (see `CommandFormatterTest`, `TimeTrackingItemQueriesTest`, `STTItemWriterTest`):

```kotlin
@RunWith(Theories::class)
class SomeTheoryTest {
    @Theory
    fun shouldHandleCase(@TestedOn(ints = [0, 1, 42]) input: Int) { ... }
}
```

### Coverage Landscape (as of July 2026)

| Area | Coverage | Details |
|------|----------|---------|
| `model/` | Full | TimeTrackingItem (22 tests), ReportingItem tested via reporting |
| `query/` | Full | TimeTrackingItemQueries (20+ tests with theories) |
| `command/` | Full | Activities (8 tests), CommandFormatter (theory-based) |
| `reporting/` | Full | Summing, Overtime, WorkingtimeItemProvider all covered |
| `time/` | Good | DurationRounder (5 tests), DateTimes (18 tests), Interval (8 tests) |
| `text/` | Partial | CommonPrefixGrouper, JiraExpansionProvider tested; ItemCategorizer via reporting |
| `persistence/` | Partial | BackupCreator, STTItemConverter, STTItemReader/Writer tested; ItemPersister untested |
| `cli/` | Partial | Main, ReportPrinter tested; CLIApplication, FormatConverter untested |
| `config/` | Minimal | Only PasswordSetting tested (2 tests); YamlConfigService etc. untested |
| `gui/jfx/` | Minimal | ActivitiesController, TimeTrackingItemCell, ReportBinding tested; most controllers untested |
| `event/` | None | ItemLogService untested |
| `submit/` | None | Entire submit package untested |
| `root/` | Added | Strings, States, IntRange tested; StopWatch, Streams, Service untested |

### Test Helpers

- **`Tests.kt`** (`org.stt.Matchers`) — `argThat(lambda)` and `any<T>()` wrappers for Mockito-Kotlin compatibility
- **`ItemReaderTestHelper.kt`** — `givenReaderReturns(reader, item1, item2, ...)` chains stubs to return items then `null`
- **`IOUtil.kt`** (`org.stt.importer`) — `readAll(reader)` collects all items from an `ItemReader`
- **`TestFX.kt`** (`org.stt.gui.jfx`) — `installTK()` mocks JavaFX `Toolkit` for headless UI testing

### Cross-Platform Requirement

Every test **must** run and pass on macOS, Linux, and Windows. Avoid:
- Hard-coding path separators (`/` or `\`) — use `File.separator` or `File(path).isAbsolute`
- Assuming a specific filesystem root (e.g. `/tmp` or `C:\`)
- Platform-specific shell commands or process execution
- Relying on Linux/macOS-only tools or paths

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

## OpenSpec Workflow

All new features and significant changes **must** start with OpenSpec — a spec-driven planning workflow that produces design artifacts before any code is written.

### Workflow Phases

```
Explore ──→ Propose ──→ Apply ──→ Archive
  │            │            │          │
  │            v            │          v
  └─── think    design       code      sync specs
  (optional)   + spec       tasks     + archive
```

### Available Skills

| Skill | Command | When to Use |
|-------|---------|-------------|
| `openspec-explore` | `/opsx-explore` | Thinking through an idea, investigating codebase, clarifying requirements before committing to a change |
| `openspec-propose` | `/opsx-propose` | Ready to formalize — creates `proposal.md`, `specs/`, `design.md`, `tasks.md` |
| `openspec-apply-change` | `/opsx-apply` | Implement tasks from a change, one by one |
| `openspec-update-change` | `/opsx-update` | Revise planning artifacts mid-change without editing code |
| `openspec-sync-specs` | `/opsx-sync` | Merge delta specs from a change into main specs |
| `openspec-archive-change` | `/opsx-archive` | Finalize a completed change and archive it |

### Starting a New Feature

1. **Explore** — Use `/opsx-explore` to investigate the codebase, clarify the problem, and explore options with the AI as a thinking partner.
2. **Propose** — Use `/opsx-propose <change-name>` to generate planning artifacts: a proposal, capability specs (delta against main specs), design decisions, and implementation tasks.
3. **Apply** — Use `/opsx-apply <change-name>` to implement tasks. Tasks are checked off in `tasks.md` as they're completed.
4. **Archive** — Use `/opsx-archive <change-name>` when all tasks are done. Delta specs are synced into main specs and the change directory is moved to `archive/`.

### Artifact Layout (spec-driven schema)

```
openspec/
├── config.yaml              # Project context, rules, tool config
├── specs/                   # Main specs (authoritative requirements)
│   └── <capability>/spec.md
└── changes/
    └── <change-name>/
        ├── .openspec.yaml   # Change metadata
        ├── proposal.md      # What & why
        ├── specs/           # Delta specs for this change
        │   └── <capability>/spec.md
        ├── design.md        # How
        └── tasks.md         # Implementation checklist
```

### Rules

- **Never skip OpenSpec** for features, enhancements, or fixes that touch externally observable behavior
- Minor refactors, dependency bumps, and documentation-only changes may bypass the workflow (use conventional commit directly)
- All OpenSpec artifacts live under `openspec/` in the repo root