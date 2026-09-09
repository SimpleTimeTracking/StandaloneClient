package org.stt.submit.csv

import org.stt.gui.jfx.ReportController.ReportListItem
import org.stt.model.TimeTrackingItem
import org.stt.reporting.SummingReportGenerator.Report
import org.stt.submit.ConnectorConfig
import org.stt.submit.SubmitConnector
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named

/**
 * [SubmitConnector] (id `csv-items`) writing individual [TimeTrackingItem]s to
 * `.stt/submit-items.csv` as a pivot table: one row per item and one column per day with
 * tracked time, durations in `HH:mm` format. Midnight-spanning items are split across their
 * day columns. [submitSummary] expands the given [ReportListItem]s back into their individual
 * items, so both entry points produce one row per item. The file is truncated and rewritten on
 * every submit (snapshot model, same as [org.stt.submit.json.JsonSubmitConnector]).
 */
class CsvItemsSubmitConnector @Inject
constructor(config: ConnectorConfig, @Named("homePath") homePath: String) : SubmitConnector {

    private val outputFile: File

    init {
        val filePath = config.file
        outputFile = if (File(filePath).isAbsolute) {
            File(filePath)
        } else {
            File(homePath, filePath)
        }
        outputFile.parentFile.mkdirs()
    }

    override val id: String = "csv-items"

    override fun submitItems(items: List<TimeTrackingItem>) {
        val rows = items.map { item -> item.activity to PerDayDurations.split(item) }
        writeCsv(CsvPivotWriter.buildCsv("activity", rows))
        LOG.info("Submitted ${items.size} items to $outputFile")
    }

    override fun submitSummary(report: Report, selectedItems: List<ReportListItem>) {
        submitItems(selectedItems.flatMap { it.backingItems })
    }

    private fun writeCsv(csv: String) {
        Files.write(outputFile.toPath(), csv.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    companion object {
        private val LOG = Logger.getLogger(CsvItemsSubmitConnector::class.java.name)
    }
}
