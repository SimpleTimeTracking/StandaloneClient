package org.stt.submit.csv

import org.stt.model.TimeTrackingItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Splits the tracked time of a [TimeTrackingItem] into per-day portions, so that
 * midnight-spanning or multi-day items credit each day with the time actually tracked on it.
 * Days without tracked time (including zero-length portions) are omitted; items without an end
 * are treated as running until now.
 */
object PerDayDurations {

    fun split(item: TimeTrackingItem): Map<LocalDate, Duration> {
        val end = item.end ?: LocalDateTime.now()
        if (!end.isAfter(item.start)) {
            return emptyMap()
        }
        val result = mutableMapOf<LocalDate, Duration>()
        var current = item.start
        while (current.toLocalDate() != end.toLocalDate()) {
            val dayEnd = current.toLocalDate().plusDays(1).atStartOfDay()
            result.merge(current.toLocalDate(), Duration.between(current, dayEnd)) { a, b -> a.plus(b) }
            current = dayEnd
        }
        result.merge(end.toLocalDate(), Duration.between(current, end)) { a, b -> a.plus(b) }
        return result.filterValues { !it.isZero }
    }
}
