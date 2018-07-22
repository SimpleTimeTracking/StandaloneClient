package org.stt.validation

import org.stt.query.Criteria
import org.stt.query.TimeTrackingItemQueries
import org.stt.time.DateTimes
import org.stt.time.until
import java.time.LocalDateTime
import java.util.Objects.requireNonNull
import javax.inject.Inject

class ItemAndDateValidator @Inject
constructor(timeTrackingItemQueries: TimeTrackingItemQueries) {
    private val timeTrackingItemQueries: TimeTrackingItemQueries

    init {
        this.timeTrackingItemQueries = requireNonNull(timeTrackingItemQueries)
    }

    fun validateItemIsFirstItemAndLater(start: LocalDateTime): Boolean {
        if (!DateTimes.isToday(start)) {
            return true
        }
        val startOfDay = start.toLocalDate().atStartOfDay()
        val searchInterval = startOfDay until start
        val criteria = Criteria()
        criteria.withStartBetween(searchInterval)
        val hasEarlierItem = timeTrackingItemQueries.queryItems(criteria)
                .findAny()
                .isPresent
        return if (hasEarlierItem) {
            true
        } else !LocalDateTime.now().isBefore(start)
    }
}
