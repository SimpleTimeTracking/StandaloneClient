package org.stt.submit.json

import org.stt.model.TimeTrackingItem
import org.stt.reporting.SummingReportGenerator.Report
import org.stt.submit.ConnectorConfig
import org.stt.submit.SubmitConnector
import org.stt.submit.SubmitStatusTracker
import org.stt.gui.jfx.ReportController.ReportListItem
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named

/**
 * [SubmitConnector] (id `json`) writing the submitted items or summary rows as a JSON array to
 * the configured output file. The file is truncated and rewritten on every submit (snapshot
 * model). Items are marked as submitted only after the JSON file was written successfully.
 */
class JsonSubmitConnector @Inject
constructor(config: ConnectorConfig, @Named("homePath") homePath: String,
            private val submitStatusTracker: SubmitStatusTracker) : SubmitConnector {

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

    override val id: String = "json"

    override fun submitItems(items: List<TimeTrackingItem>) {
        val json = buildJsonArray(items.map { itemToMap(it) })
        writeJson(json)
        items.forEach { submitStatusTracker.markSubmitted(it, id) }
        LOG.info("Submitted ${items.size} items to $outputFile")
    }

    override fun submitSummary(report: Report, selectedItems: List<ReportListItem>) {
        val items = mutableListOf<Map<String, Any?>>()
        for (reportItem in selectedItems) {
            items.add(mapOf(
                "comment" to reportItem.comment,
                "isBreak" to reportItem.isBreak,
                "duration" to reportItem.duration.toString(),
                "roundedDuration" to reportItem.roundedDuration.toString()
            ))
        }
        val json = buildJsonArray(items)
        writeJson(json)
        selectedItems.flatMap { it.backingItems }.forEach { submitStatusTracker.markSubmitted(it, id) }
        LOG.info("Submitted ${selectedItems.size} summary items to $outputFile")
    }

    private fun itemToMap(item: TimeTrackingItem): Map<String, Any?> {
        return mapOf(
            "activity" to item.activity,
            "start" to item.start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "end" to item.end?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "submittedAt" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    private fun buildJsonArray(items: List<Map<String, Any?>>): String {
        val sb = StringBuilder()
        sb.appendLine("[")
        for (i in items.indices) {
            val item = items[i]
            sb.appendLine("  {")
            val entries = item.entries.toList()
            for (j in entries.indices) {
                val (key, value) = entries[j]
                sb.append("    \"$key\": ")
                when (value) {
                    null -> sb.append("null")
                    is String -> sb.append("\"$value\"")
                    is Number -> sb.append(value)
                    is Boolean -> sb.append(value)
                    else -> sb.append("\"$value\"")
                }
                if (j < entries.size - 1) sb.append(",")
                sb.appendLine()
            }
            sb.append("  }")
            if (i < items.size - 1) sb.append(",")
            sb.appendLine()
        }
        sb.append("]")
        return sb.toString()
    }

    private fun writeJson(json: String) {
        Files.write(outputFile.toPath(), json.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    companion object {
        private val LOG = Logger.getLogger(JsonSubmitConnector::class.java.name)
    }
}