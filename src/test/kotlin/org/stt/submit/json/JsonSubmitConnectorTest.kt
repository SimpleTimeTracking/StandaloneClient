package org.stt.submit.json

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
 * Tests for [JsonSubmitConnector].
 *
 * Two concerns:
 * 1. [JsonSubmitConnector.submitItems] — serializes [TimeTrackingItem]s to a JSON file
 * 2. [JsonSubmitConnector.submitSummary] — serializes report list items to a JSON file
 *
 * Verifies correct JSON structure, field values, null handling, absolute vs relative paths,
 * file overwrite behaviour, and the connector id property.
 */

class JsonSubmitConnectorTest {

    @field:Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    private lateinit var homePath: String
    private lateinit var sut: JsonSubmitConnector

    @Before
    fun setup() {
        homePath = tempFolder.newFolder("home").absolutePath
        val config = ConnectorConfig(type = "json", file = ".stt/submit.json")
        sut = JsonSubmitConnector(config, homePath)
    }

    @Test
    fun shouldCreateJsonFileOnSubmitItems() {
        // GIVEN
        val item = TimeTrackingItem("test activity", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        val outputFile = File(homePath, ".stt/submit.json")
        assertThat(outputFile).exists()
    }

    @Test
    fun shouldWriteCorrectJsonContentForItems() {
        // GIVEN
        val item = TimeTrackingItem("test activity", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        val outputFile = File(homePath, ".stt/submit.json")
        val content = String(Files.readAllBytes(outputFile.toPath()))
        assertThat(content).contains("\"activity\": \"test activity\"")
        assertThat(content).contains("\"start\": \"2020-01-01T10:00:00\"")
        assertThat(content).contains("\"end\": \"2020-01-01T11:00:00\"")
        assertThat(content).contains("\"submittedAt\"")
    }

    @Test
    fun shouldWriteMultipleItemsAsArray() {
        // GIVEN
        val item1 = TimeTrackingItem("a", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("b", LocalDateTime.of(2020, 1, 1, 12, 0), LocalDateTime.of(2020, 1, 1, 13, 0))

        // WHEN
        sut.submitItems(listOf(item1, item2))

        // THEN
        val outputFile = File(homePath, ".stt/submit.json")
        val content = String(Files.readAllBytes(outputFile.toPath()))
        assertThat(content).startsWith("[")
        assertThat(content).endsWith("]")
        assertThat(content).contains("\"activity\": \"a\"")
        assertThat(content).contains("\"activity\": \"b\"")
    }

    @Test
    fun shouldHandleItemWithoutEnd() {
        // GIVEN
        val item = TimeTrackingItem("ongoing", LocalDateTime.of(2020, 1, 1, 10, 0))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        val outputFile = File(homePath, ".stt/submit.json")
        val content = String(Files.readAllBytes(outputFile.toPath()))
        assertThat(content).contains("\"end\": null")
    }

    @Test
    fun shouldCreateJsonFileOnSubmitSummary() {
        // GIVEN
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("summary item", false, Duration.ofHours(2), Duration.ofHours(2))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        val outputFile = File(homePath, ".stt/submit.json")
        assertThat(outputFile).exists()
    }

    @Test
    fun shouldWriteCorrectJsonForSummary() {
        // GIVEN
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("summary item", false, Duration.ofHours(2), Duration.ofMinutes(119))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        val outputFile = File(homePath, ".stt/submit.json")
        val content = String(Files.readAllBytes(outputFile.toPath()))
        assertThat(content).contains("\"comment\": \"summary item\"")
        assertThat(content).contains("\"isBreak\": false")
        assertThat(content).contains("\"duration\": \"PT2H\"")
        assertThat(content).contains("\"roundedDuration\": \"PT1H59M\"")
    }

    @Test
    fun shouldWriteBreakItemInSummary() {
        // GIVEN
        val report = SummingReportGenerator.Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)
        val reportItem = ReportController.ReportListItem("break comment", true, Duration.ofMinutes(30), Duration.ofMinutes(30))

        // WHEN
        sut.submitSummary(report, listOf(reportItem))

        // THEN
        val outputFile = File(homePath, ".stt/submit.json")
        val content = String(Files.readAllBytes(outputFile.toPath()))
        assertThat(content).contains("\"isBreak\": true")
    }

    @Test
    fun shouldUseAbsolutePathWhenFileStartsWithSlash() {
        // GIVEN
        val absolutePath = File(tempFolder.newFolder("custom"), "output.json").absolutePath
        val config = ConnectorConfig(type = "json", file = absolutePath)
        val connector = JsonSubmitConnector(config, homePath)
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        connector.submitItems(listOf(item))

        // THEN
        val outputFile = File(absolutePath)
        assertThat(outputFile).exists()
        val content = String(Files.readAllBytes(outputFile.toPath()))
        assertThat(content).contains("\"activity\": \"test\"")
    }

    @Test
    fun shouldOverwriteExistingFileOnEachSubmit() {
        // GIVEN
        val item1 = TimeTrackingItem("first", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("second", LocalDateTime.of(2020, 1, 1, 12, 0), LocalDateTime.of(2020, 1, 1, 13, 0))
        sut.submitItems(listOf(item1))

        // WHEN
        sut.submitItems(listOf(item2))

        // THEN
        val outputFile = File(homePath, ".stt/submit.json")
        val content = String(Files.readAllBytes(outputFile.toPath()))
        assertThat(content).contains("\"activity\": \"second\"")
        assertThat(content).doesNotContain("\"activity\": \"first\"")
    }

    @Test
    fun shouldHaveCorrectId() {
        assertThat(sut.id).isEqualTo("json")
    }
}