package org.stt.query

import org.stt.model.TimeTrackingItem
import org.stt.time.Interval
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * All present conditions in the clause must be valid for a match.
 */
class Criteria {
    private var startNotBefore = LocalDateTime.MIN
    private var startBefore = LocalDateTime.MAX
    private var endNotAfter: LocalDateTime? = null
    private var endBefore: LocalDateTime? = null
    private var startsAt: LocalDateTime? = null
    private var endsAt: LocalDateTime? = null
    private var activityContains = ""
    private var activityIs: String? = null
    private var activityIsNot: String? = null

    fun withStartBetween(interval: Interval): Criteria {
        withStartNotBefore(interval.start)
        withStartBefore(interval.end)
        return this
    }

    fun withStartNotBefore(time: LocalDateTime): Criteria {
        startNotBefore = time
        return this
    }

    fun withStartBefore(time: LocalDateTime): Criteria {
        startBefore = time
        return this
    }

    fun withEndNotAfter(time: LocalDateTime): Criteria {
        endNotAfter = time
        return this
    }

    fun withPeriodAtDay(date: LocalDate): Criteria {
        val startOfDayAtDate = date.atStartOfDay()
        withStartNotBefore(startOfDayAtDate)
        withStartBefore(startOfDayAtDate.plusDays(1))
        return this
    }

    fun withEndBefore(time: LocalDateTime): Criteria {
        endBefore = time
        return this
    }

    fun withActivityContains(substring: String): Criteria {
        activityContains = substring
        return this
    }

    fun withActivityIsNot(activity: String) {
        activityIsNot = activity
    }

    fun withStartsAt(start: LocalDateTime): Criteria {
        startsAt = start
        return this
    }

    fun withActivityIs(activity: String): Criteria {
        activityIs = activity
        return this
    }

    fun withEndsAt(end: LocalDateTime): Criteria {
        endsAt = end
        return this
    }

    fun matches(item: TimeTrackingItem): Boolean {
        Objects.requireNonNull(item)
        if (!item.start.isBefore(startBefore)) {
            return false
        }
        if (item.start.isBefore(startNotBefore)) {
            return false
        }
        if (endNotAfter != null && item.end?.isAfter(endNotAfter) != false) {
            return false
        }
        if (endBefore != null && item.end?.isBefore(endBefore) != true) {
            return false
        }
        if (!item.activity.contains(activityContains)) {
            return false
        }
        if (startsAt != null && item.start != startsAt) {
            return false
        }
        if (endsAt != null && endsAt != item.end) {
            return false
        }
        if (activityIs != null && activityIs != item.activity) {
            return false
        }
        return activityIsNot != item.activity
    }
}
