package org.stt.submit.csv

import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds the pivot-table CSV serialization shared by [CsvItemsSubmitConnector] and
 * [CsvSummarySubmitConnector]: a header row (first cell [headerLabel], then one `yyyy-MM-dd`
 * column per day with tracked time) followed by one row per entry holding `HH:mm` durations in
 * the matching day columns (`00:00` for days without tracked time in that row).
 */
internal object CsvPivotWriter {

    fun formatDuration(duration: Duration): String {
        val totalMinutes = duration.toMinutes()
        return String.format(Locale.ROOT, "%02d:%02d", totalMinutes / 60, totalMinutes % 60)
    }

    fun buildCsv(headerLabel: String, rows: List<Pair<String, Map<LocalDate, Duration>>>): String {
        val days = rows.flatMap { it.second.keys }.toSortedSet()
        val sb = StringBuilder()
        sb.append(CsvUtil.escapeCell(headerLabel))
        for (day in days) {
            sb.append(',').append(day.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
        sb.append('\n')
        for ((label, dayDurations) in rows) {
            sb.append(CsvUtil.escapeCell(label))
            for (day in days) {
                sb.append(',').append(formatDuration(dayDurations[day] ?: Duration.ZERO))
            }
            sb.append('\n')
        }
        return sb.toString()
    }
}
