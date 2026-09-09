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
 * Tests for [CsvSummarySubmitConnector].
 *
 * Verifies grouped pivot-table CSV output: one row per comment, per-day duration aggregation
 * (after midnight splitting), HH:mm format, yyyy-MM-dd columns, file overwrite on each submit,
 * submitItems() grouping of raw items, and the connector id.
 *
 * Also simulates update behaviour: submitting summary rows, adding more items for the same
 * comment (or new comments, or new days), and submitting again — rows must be replaced, not
 * appended; durations must be re-aggregated from the latest submission only; day columns grow
 * with the submitted data; comments missing from the second submission disappear.
 */
class CsvSummarySubmitConnectorTest {

    @field:Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    private lateinit var homePath: String
    private lateinit var sut: CsvSummarySubmitConnector

    @Before
    fun setup() {
        homePath = tempFolder.newFolder("home").absolutePath
        val config = ConnectorConfig(type = "csv-summary", file = ".stt/submit-summary.csv")
        sut = CsvSummarySubmitConnector(config, homePath)
    }

    @Test
    fun shouldHaveCorrectId() {
        assertThat(sut.id).isEqualTo("csv-summary")
    }

    @Test
    fun shouldCreateCsvFileOnSubmitSummary() {
        // GIVEN
        val backingItem = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("work", false, Duration.ofHours(2), Duration.ofHours(2), listOf(backingItem))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        assertThat(File(homePath, ".stt/submit-summary.csv")).exists()
    }

    @Test
    fun shouldWriteSingleSummaryRowWithAggregatedDuration() {
        // GIVEN
        val backingItem = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 30))
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("work", false, Duration.ofMinutes(90), Duration.ofMinutes(90), listOf(backingItem))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01\nwork,01:30\n")
    }

    @Test
    fun shouldAggregateDurationsOfSameCommentOnSameDay() {
        // GIVEN
        val backingItem1 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        val backingItem2 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 11, 0), LocalDateTime.of(2020, 1, 1, 13, 45))
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("work", false, Duration.ofMinutes(165), Duration.ofMinutes(165), listOf(backingItem1, backingItem2))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01\nwork,03:45\n")
    }

    @Test
    fun shouldWriteOneRowPerCommentForMultipleSummaryRows() {
        // GIVEN
        val workItem = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        val meetingItem = TimeTrackingItem("meeting", LocalDateTime.of(2020, 1, 2, 9, 0), LocalDateTime.of(2020, 1, 2, 10, 0))
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val workRow = ReportController.ReportListItem("work", false, Duration.ofHours(1), Duration.ofHours(1), listOf(workItem))
        val meetingRow = ReportController.ReportListItem("meeting", false, Duration.ofHours(1), Duration.ofHours(1), listOf(meetingItem))

        // WHEN
        sut.submitSummary(report, listOf(workRow, meetingRow))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01,2020-01-02\nwork,01:00,00:00\nmeeting,00:00,01:00\n")
    }

    @Test
    fun shouldSplitMidnightSpanBeforeAggregation() {
        // GIVEN
        val nightItem = TimeTrackingItem("night shift", LocalDateTime.of(2020, 1, 1, 22, 0), LocalDateTime.of(2020, 1, 2, 2, 0))
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("night shift", false, Duration.ofHours(4), Duration.ofHours(4), listOf(nightItem))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01,2020-01-02\nnight shift,02:00,02:00\n")
    }

    @Test
    fun shouldGroupRawItemsByCommentOnSubmitItems() {
        // GIVEN
        val workItem1 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        val meetingItem = TimeTrackingItem("meeting", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 10, 30))
        val workItem2 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 11, 0), LocalDateTime.of(2020, 1, 1, 12, 0))

        // WHEN
        sut.submitItems(listOf(workItem1, meetingItem, workItem2))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01\nwork,02:00\nmeeting,00:30\n")
    }

    @Test
    fun shouldOverwriteExistingFileOnEachSubmit() {
        // GIVEN
        val firstItem = TimeTrackingItem("first", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val firstRow = ReportController.ReportListItem("first", false, Duration.ofHours(1), Duration.ofHours(1), listOf(firstItem))
        sut.submitSummary(report, listOf(firstRow))

        // WHEN
        val secondItem = TimeTrackingItem("second", LocalDateTime.of(2020, 1, 1, 11, 0), LocalDateTime.of(2020, 1, 1, 12, 0))
        val secondRow = ReportController.ReportListItem("second", false, Duration.ofHours(1), Duration.ofHours(1), listOf(secondItem))
        sut.submitSummary(report, listOf(secondRow))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01\nsecond,01:00\n")
    }

    @Test
    fun shouldEscapeCommentWithComma() {
        // GIVEN
        val item = TimeTrackingItem("work, urgent", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("work, urgent", false, Duration.ofHours(1), Duration.ofHours(1), listOf(item))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01\n\"work, urgent\",01:00\n")
    }

    @Test
    fun shouldReplaceExistingRowWhenSubmittingAgainWithMoreItemsForSameComment() {
        // GIVEN
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val firstItem = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        sut.submitSummary(report, listOf(ReportController.ReportListItem("work", false, Duration.ofHours(1), Duration.ofHours(1), listOf(firstItem))))

        // WHEN
        val secondItem = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 11, 0), LocalDateTime.of(2020, 1, 1, 13, 30))
        sut.submitSummary(report, listOf(ReportController.ReportListItem("work", false, Duration.ofHours(2), Duration.ofHours(2), listOf(firstItem, secondItem))))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01\nwork,03:30\n")
        assertThat(content.lines().count { it.startsWith("work,") }).isEqualTo(1)
    }

    @Test
    fun shouldGrowDayColumnsWhenSubmittingAgainWithItemsOnAdditionalDay() {
        // GIVEN
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val firstItem = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        sut.submitSummary(report, listOf(ReportController.ReportListItem("work", false, Duration.ofHours(1), Duration.ofHours(1), listOf(firstItem))))

        // WHEN
        val secondItem = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 2, 9, 0), LocalDateTime.of(2020, 1, 2, 11, 0))
        sut.submitSummary(report, listOf(ReportController.ReportListItem("work", false, Duration.ofHours(3), Duration.ofHours(3), listOf(firstItem, secondItem))))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01,2020-01-02\nwork,01:00,02:00\n")
    }

    @Test
    fun shouldUpdateSplitDurationsWhenSubmittingAgainAfterMidnightItemWasAdded() {
        // GIVEN
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val dayItem = TimeTrackingItem("night shift", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        sut.submitSummary(report, listOf(ReportController.ReportListItem("night shift", false, Duration.ofHours(1), Duration.ofHours(1), listOf(dayItem))))

        // WHEN
        val nightItem = TimeTrackingItem("night shift", LocalDateTime.of(2020, 1, 1, 22, 0), LocalDateTime.of(2020, 1, 2, 2, 0))
        sut.submitSummary(report, listOf(ReportController.ReportListItem("night shift", false, Duration.ofHours(5), Duration.ofHours(5), listOf(dayItem, nightItem))))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01,2020-01-02\nnight shift,03:00,02:00\n")
    }

    @Test
    fun shouldDropRowsForCommentsMissingFromSecondSubmission() {
        // GIVEN
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val workItem = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        val meetingItem = TimeTrackingItem("meeting", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.submitSummary(report, listOf(
            ReportController.ReportListItem("work", false, Duration.ofHours(1), Duration.ofHours(1), listOf(workItem)),
            ReportController.ReportListItem("meeting", false, Duration.ofHours(1), Duration.ofHours(1), listOf(meetingItem))))

        // WHEN
        sut.submitSummary(report, listOf(ReportController.ReportListItem("work", false, Duration.ofHours(1), Duration.ofHours(1), listOf(workItem))))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01\nwork,01:00\n")
    }

    @Test
    fun shouldRegroupRawItemsWhenSubmittingAgainWithAdditionalRawItems() {
        // GIVEN
        val workItem1 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 10, 0))
        sut.submitItems(listOf(workItem1))

        // WHEN
        val workItem2 = TimeTrackingItem("work", LocalDateTime.of(2020, 1, 1, 11, 0), LocalDateTime.of(2020, 1, 1, 11, 30))
        val meetingItem = TimeTrackingItem("meeting", LocalDateTime.of(2020, 1, 1, 12, 0), LocalDateTime.of(2020, 1, 1, 12, 15))
        sut.submitItems(listOf(workItem1, workItem2, meetingItem))

        // THEN
        val content = readFileContent()
        assertThat(content).isEqualTo("comment,2020-01-01\nwork,01:30\nmeeting,00:15\n")
        assertThat(content.lines().count { it.startsWith("work,") }).isEqualTo(1)
    }

    private fun readFileContent(): String {
        val outputFile = File(homePath, ".stt/submit-summary.csv")
        return String(Files.readAllBytes(outputFile.toPath()))
    }
}
