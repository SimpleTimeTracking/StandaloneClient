package org.stt.reporting

import org.stt.model.ReportingItem
import org.stt.model.TimeTrackingItem
import org.stt.text.ItemCategorizer
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.Comparator.comparing
import java.util.stream.Stream

/**
 * Reads all elements from the given reader and groups by the comment of the
 * item: all items with the identical comment get merged into one
 * [ReportingItem]. Duration is the sum of all durations of the items.
 *
 *
 * Items without an end date get reported as if the end date was now
 *
 *
 * Items will be returned sorted in ascending order of the comments
 */
class SummingReportGenerator(
    private val itemsToRead: Stream<TimeTrackingItem>,
    private val itemCategorizer: ItemCategorizer
) {

    fun createReport(): Report {
        var startOfReport: LocalDateTime? = null
        var endOfReport: LocalDateTime? = null
        val reportList = LinkedList<ReportingItem>()


        val collectingMap = HashMap<String, Duration>()
        var uncoveredDuration = Duration.ZERO
        var lastItem: TimeTrackingItem? = null
        itemsToRead.use { items ->
            val it = items.iterator()
            while (it.hasNext()) {
                val item = it.next()
                val now = LocalDateTime.now()
                val start = item.start
                val end = item.end ?: now

                if (lastItem != null) {
                    val endOfLastItem = lastItem!!.end ?: now
                    if (endOfLastItem.isBefore(start)) {
                        val additionalUncoveredTime = Duration.between(
                            endOfLastItem, start
                        )
                        uncoveredDuration = uncoveredDuration
                            .plus(additionalUncoveredTime)
                    }
                }

                lastItem = item

                if (startOfReport == null) {
                    startOfReport = start
                }
                endOfReport = end

                var duration = Duration.between(start, end)
                if (duration.isNegative) {
                    duration = Duration.ZERO
                }
                val comment = item.activity
                if (collectingMap.containsKey(comment)) {
                    val oldDuration = collectingMap[comment]
                    collectingMap[comment] = oldDuration!!.plus(duration)
                } else {
                    collectingMap[comment] = duration
                }
            }

        }

        for ((comment, duration) in collectingMap) {
            val isBreak = ItemCategorizer.ItemCategory.BREAK == itemCategorizer.getCategory(comment)
            reportList.add(ReportingItem(duration, comment, isBreak))
        }

        reportList.sortWith(comparing<ReportingItem, String> { it.comment })
        return Report(
            reportList, startOfReport, endOfReport,
            uncoveredDuration
        )
    }


    class Report(
        val reportingItems: List<ReportingItem>,
        val start: LocalDateTime?,
        val end: LocalDateTime?,
        val uncoveredDuration: Duration
    )
}
