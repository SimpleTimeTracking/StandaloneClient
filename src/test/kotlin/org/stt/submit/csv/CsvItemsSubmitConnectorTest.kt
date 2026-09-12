package org.stt.submit.csv

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.stt.gui.jfx.ReportController
import org.stt.model.TimeTrackingItem
import org.stt.reporting.SummingReportGenerator
import org.stt.submit.ConnectorConfig
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.LocalDateTime

/**
 * Tests for [CsvItemsSubmitConnector].
 *
 * Verifies pivot-table CSV output: header with day columns (yyyy-MM-dd), one row per item,
 * HH:mm durations in the correct day column, midnight splitting, only days with tracked time,
 * file overwrite on each submit, summary expansion via backingItems, and the connector id.
 *
 * Also simulates update behaviour: submitting items, adding more items, and submitting again —
 * the file must always reflect exactly the latest submission (no duplicated or stale rows,
 * day columns growing/shrinking with the submitted data).
 */
class CsvItemsSubmitConnectorTest {

    @field:Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    private lateinit var homePath: String
    private lateinit var sut: CsvItemsSubmitConnector

    @Before
    fun setup() {
        homePath = tempFolder.newFolder("home").absolutePath
        val config = ConnectorConfig(type = "csv-items", file = ".stt/submit-items.csv")
        sut = CsvItemsSubmitConnector(config, homePath)
    }

    @Test
    fun shouldHaveCorrectId() {
        assertThat(sut.id).isEqualTo("csv-items")
    }

    @Test
    fun shouldCreateCsvFileOnSubmitItems() {
        // GIVEN
        val item = TimeTrackingItem("test activity", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        assertThat(File(homePath, ".stt/submit-items.csv")).exists()
    }

    @Test
    fun shouldWriteSingleItemAsRowWithDurationInDayColumn() {
        // GIVEN
        val item = TimeTrackingItem("test activity", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 30))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01\ntest activity,01:30\n")
    }

    @Test
    fun shouldWriteOneRowPerItemForSameDayItems() {
        // GIVEN
        val item1 = TimeTrackingItem("first", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("second", LocalDateTime.of(2020, 1, 1, 12, 0), LocalDateTime.of(2020, 1, 1, 13, 0))

        // WHEN
        sut.submitItems(listOf(item1, item2))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01\nfirst,01:00\nsecond,01:00\n")
    }

    @Test
    fun shouldSplitItemSpanningMidnightIntoDayColumns() {
        // GIVEN
        val item = TimeTrackingItem("night shift", LocalDateTime.of(2020, 1, 1, 22, 0), LocalDateTime.of(2020, 1, 2, 2, 0))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01,2020-01-02\nnight shift,02:00,02:00\n")
    }

    @Test
    fun shouldOnlyEmitColumnsForDaysWithTrackedTime() {
        // GIVEN
        val item1 = TimeTrackingItem("a", LocalDateTime.of(2026, 8, 31, 10, 0), LocalDateTime.of(2026, 8, 31, 11, 0))
        val item2 = TimeTrackingItem("b", LocalDateTime.of(2026, 9, 3, 10, 0), LocalDateTime.of(2026, 9, 3, 11, 0))

        // WHEN
        sut.submitItems(listOf(item1, item2))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2026-08-31,2026-09-03\na,01:00,00:00\nb,00:00,01:00\n")
        assertThat(content).doesNotContain("2026-09-01")
        assertThat(content).doesNotContain("2026-09-02")
    }

    @Test
    fun shouldEscapeActivityWithComma() {
        // GIVEN
        val item = TimeTrackingItem("activity, with comma", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01\n\"activity, with comma\",01:00\n")
    }

    @Test
    fun shouldOverwriteExistingFileOnEachSubmit() {
        // GIVEN
        val item1 = TimeTrackingItem("first", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("second", LocalDateTime.of(2020, 1, 2, 10, 0), LocalDateTime.of(2020, 1, 2, 11, 0))
        sut.submitItems(listOf(item1))

        // WHEN
        sut.submitItems(listOf(item2))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-02\nsecond,01:00\n")
    }

    @Test
    fun shouldExpandBackingItemsOnSubmitSummary() {
        // GIVEN
        val backingItem1 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        val backingItem2 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 11, 0), LocalDateTime.of(2020, 1, 1, 12, 0))
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("work", false, Duration.ofHours(2), Duration.ofHours(2), listOf(backingItem1, backingItem2))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01\nwork,01:00\nwork,01:00\n")
    }

    @Test
    fun shouldReplaceRowsWhenSubmittingAgainWithExtendedItemSet() {
        // GIVEN
        val item1 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        sut.submitItems(listOf(item1))

        // WHEN
        val item2 = TimeTrackingItem("meeting", LocalDateTime.of(2020, 1, 1, 11, 0), LocalDateTime.of(2020, 1, 1, 12, 0))
        sut.submitItems(listOf(item1, item2))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01\nwork,01:00\nmeeting,01:00\n")
        assertThat(content.lines().count { it.startsWith("work,") }).isEqualTo(1)
    }

    @Test
    fun shouldContainOnlyLatestItemsWhenSubmittingAgainWithFewerItems() {
        // GIVEN
        val item1 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        val item2 = TimeTrackingItem("meeting", LocalDateTime.of(2020, 1, 1, 11, 0), LocalDateTime.of(2020, 1, 1, 12, 0))
        sut.submitItems(listOf(item1, item2))

        // WHEN
        sut.submitItems(listOf(item1))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01\nwork,01:00\n")
        assertThat(content).doesNotContain("meeting")
    }

    @Test
    fun shouldGrowDayColumnsWhenSubmittingAgainWithItemsOnNewDay() {
        // GIVEN
        val item1 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        sut.submitItems(listOf(item1))

        // WHEN
        val item2 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 2, 9, 0), LocalDateTime.of(2020, 1, 2, 10, 0))
        sut.submitItems(listOf(item1, item2))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01,2020-01-02\nwork,01:00,00:00\nwork,00:00,01:00\n")
    }

    @Test
    fun shouldUpdateSplitRowWhenSubmittingAgainAfterMidnightItemWasAdded() {
        // GIVEN
        val dayItem = TimeTrackingItem("night shift", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        sut.submitItems(listOf(dayItem))

        // WHEN
        val nightItem = TimeTrackingItem("night shift", LocalDateTime.of(2020, 1, 1, 22, 0), LocalDateTime.of(2020, 1, 2, 2, 0))
        sut.submitItems(listOf(dayItem, nightItem))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("activity,2020-01-01,2020-01-02\nnight shift,01:00,00:00\nnight shift,02:00,02:00\n")
    }

    private fun readFileContent(): String {
        val outputFile = File(homePath, ".stt/submit-items.csv")
        return String(Files.readAllBytes(outputFile.toPath()))
    }
}
