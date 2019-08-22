package org.stt.cli

import org.stt.model.TimeTrackingItem
import org.stt.time.DateTimes

import java.time.Duration
import java.time.LocalDateTime

internal object ItemFormattingHelper {

    fun prettyPrintItem(item: TimeTrackingItem): String {

        val builder = StringBuilder()

        val start = item.start
        val end = item.end
        val comment = item.activity

        builder.append(DateTimes.prettyPrintTime(start))

        builder.append(" - ")
        if (end == null) {
            builder.append("now     ")
        } else {
            builder.append(DateTimes.DATE_TIME_FORMATTER_HH_MM_SS.format(end))
        }
        builder.append(" ( ")
        builder.append(DateTimes.FORMATTER_PERIOD_HHh_MMm_SSs(Duration.between(start, end ?: LocalDateTime.now())))
        builder.append(" ) ")
        builder.append(" => ")
        builder.append(comment)

        return builder.toString()
    }
}
