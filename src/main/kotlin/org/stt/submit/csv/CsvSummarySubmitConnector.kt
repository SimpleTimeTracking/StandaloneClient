package org.stt.submit.csv

import org.stt.gui.jfx.ReportController.ReportListItem
import org.stt.model.TimeTrackingItem
import org.stt.reporting.SummingReportGenerator.Report
import org.stt.submit.ConnectorConfig
import org.stt.submit.SubmitConnector
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.LocalDate
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named

/**
 * [SubmitConnector] (id `csv-summary`) writing grouped summary rows to
 * `.stt/submit-summary.csv` as a pivot table: one row per comment and one column per day with
 * tracked time, durations aggregated per comment per day (after splitting midnight spans).
 * [submitItems] groups the raw items by comment internally, so both entry points produce the
 * same grouped rows. The file is truncated and rewritten on every submit (snapshot model, same
 * as [org.stt.submit.json.JsonSubmitConnector]).
 */
class CsvSummarySubmitConnector @Inject
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

    override val id: String = "csv-summary"

    override fun submitItems(items: List<TimeTrackingItem>) {
        val rows = items.groupBy { it.activity }
            .map { (comment, commentItems) -> comment to aggregatePerDay(commentItems) }
        writeCsv(rows)
        LOG.info("Submitted ${rows.size} summary rows to $outputFile")
    }

    override fun submitSummary(report: Report, selectedItems: List<ReportListItem>) {
        val rows = selectedItems.map { reportItem -> reportItem.comment to aggregatePerDay(reportItem.backingItems) }
        writeCsv(rows)
        LOG.info("Submitted ${selectedItems.size} summary rows to $outputFile")
    }

    private fun aggregatePerDay(items: List<TimeTrackingItem>): Map<LocalDate, Duration> {
        val result = mutableMapOf<LocalDate, Duration>()
        for (item in items) {
            for ((day, duration) in PerDayDurations.split(item)) {
                result.merge(day, duration) { a, b -> a.plus(b) }
            }
        }
        return result
    }

    private fun writeCsv(rows: List<Pair<String, Map<LocalDate, Duration>>>) {
        val csv = CsvPivotWriter.buildCsv("comment", rows)
        Files.write(outputFile.toPath(), csv.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    companion object {
        private val LOG = Logger.getLogger(CsvSummarySubmitConnector::class.java.name)
    }
}
