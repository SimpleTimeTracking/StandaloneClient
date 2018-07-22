package org.stt.query

import org.stt.reporting.WorkingtimeItemProvider
import org.stt.text.ItemCategorizer
import org.stt.time.Interval
import org.stt.time.asInterval
import org.stt.time.preciseToSecond
import org.stt.time.until
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkTimeQueries @Inject
constructor(private val workingtimeItemProvider: WorkingtimeItemProvider,
            private val itemCategorizer: ItemCategorizer,
            private val timeTrackingItemQueries: TimeTrackingItemQueries) {

    fun queryRemainingWorktimeToday(): Duration {
        val now = LocalDateTime.now().preciseToSecond()
        val today = now.toLocalDate()
        val workedTime = queryWorktime(today.asInterval().withEnd(now))
        return workingtimeItemProvider.getWorkingTimeFor(today).min.minus(workedTime)
    }

    fun queryWeekWorktime(): Duration {
        val now = LocalDateTime.now().preciseToSecond()
        val monday = now.toLocalDate().with(DayOfWeek.MONDAY)
        val currentWeek = monday.atStartOfDay() until now
        return queryWorktime(currentWeek)
    }

    fun queryWorktime(interval: Interval): Duration {
        val criteria = Criteria()
        criteria.withStartNotBefore(interval.start)
        criteria.withStartBefore(interval.end)
        return timeTrackingItemQueries.queryItems(criteria)
                .filter { (activity) -> itemCategorizer.getCategory(activity) == ItemCategorizer.ItemCategory.WORKTIME }
                .map { (_, start, end) ->
                    Duration.between(start, end ?: interval.end)
                }
                .reduce(Duration.ZERO, { obj, duration -> obj.plus(duration) })
    }
}
