package org.stt.submit

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.stt.config.ConfigRoot
import org.stt.submit.csv.CsvItemsSubmitConnector
import org.stt.submit.csv.CsvSummarySubmitConnector
import org.stt.submit.json.JsonSubmitConnector
import java.io.File
import java.nio.file.Files

/**
 * Tests for [SubmitModule] connector providers.
 *
 * Verifies the connector ids (`json`, `csv-items`, `csv-summary`), the default output paths for
 * the CSV connectors, and that a configured [ConnectorConfig] file overrides the default.
 */
class SubmitModuleTest {

    private val homePath = Files.createTempDirectory("stt-home").toFile().absolutePath
    private val sut = SubmitModule()

    @Test
    fun shouldProvideJsonSubmitConnector() {
        // GIVEN
        val configRoot = ConfigRoot()

        // WHEN
        val connector = sut.provideJsonSubmitConnector(configRoot, homePath)

        // THEN
        assertThat(connector.id).isEqualTo("json")
        assertThat(connector).isInstanceOf(JsonSubmitConnector::class.java)
    }

    @Test
    fun shouldProvideCsvItemsSubmitConnectorWithDefaultPath() {
        // GIVEN
        val configRoot = ConfigRoot()

        // WHEN
        val connector = sut.provideCsvItemsSubmitConnector(configRoot, homePath)

        // THEN
        assertThat(connector.id).isEqualTo("csv-items")
        assertThat(connector).isInstanceOf(CsvItemsSubmitConnector::class.java)
        val expectedFile = File(homePath, ".stt/submit-items.csv")
        assertThat(expectedFile.parentFile).exists()
    }

    @Test
    fun shouldProvideCsvSummarySubmitConnectorWithDefaultPath() {
        // GIVEN
        val configRoot = ConfigRoot()

        // WHEN
        val connector = sut.provideCsvSummarySubmitConnector(configRoot, homePath)

        // THEN
        assertThat(connector.id).isEqualTo("csv-summary")
        assertThat(connector).isInstanceOf(CsvSummarySubmitConnector::class.java)
        val expectedFile = File(homePath, ".stt/submit-summary.csv")
        assertThat(expectedFile.parentFile).exists()
    }

    @Test
    fun shouldUseConfiguredFileWhenPresent() {
        // GIVEN
        val configRoot = ConfigRoot()
        configRoot.submit = SubmitConfig(connectors = listOf(
            ConnectorConfig(type = "csv-items", file = "custom/items.csv")))

        // WHEN
        val connector = sut.provideCsvItemsSubmitConnector(configRoot, homePath) as CsvItemsSubmitConnector

        // THEN
        connector.submitItems(emptyList())
        assertThat(File(homePath, "custom/items.csv")).exists()
        assertThat(File(homePath, ".stt/submit-items.csv")).doesNotExist()
    }
}
