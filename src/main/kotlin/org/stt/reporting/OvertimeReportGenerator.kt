package org.stt.reporting

import org.stt.query.TimeTrackingItemQueries
import org.stt.text.ItemCategorizer
import org.stt.text.ItemCategorizer.ItemCategory
import org.stt.time.DateTimes
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Calculates overtime information
 */
class OvertimeReportGenerator(private val queries: TimeTrackingItemQueries,
                              private val categorizer: ItemCategorizer,
                              private val workingtimeItemProvider: WorkingtimeItemProvider) {

    val overallOvertime: Duration
        get() {
            var result = Duration.ZERO
            for (d in overtime.values) {
                result = result.plus(d)
            }
            return result
        }

    /**
     * @return the date and the according overtime (positive or negative) for
     * all elements
     */
    val overtime: Map<LocalDate, Duration>
        get() = queries.queryAllItems().use { items ->
            val dateToOvertime = TreeMap<LocalDate, Duration>()
            items.forEach { (activity, start, end) ->
                val category = categorizer.getCategory(activity)
                if (category == ItemCategory.WORKTIME) {
                    val currentDay = start.toLocalDate()
                    val currentDuration = dateToOvertime[currentDay]
                    val itemDuration = Duration.between(start, end ?: LocalDateTime.now())
                    if (currentDuration != null) {
                        dateToOvertime[currentDay] = currentDuration.plus(itemDuration)
                    } else {
                        dateToOvertime[currentDay] = itemDuration
                    }
                }
            }

            for ((key, value) in dateToOvertime) {
                dateToOvertime[key] = getOvertime(key, value)
            }

            dateToOvertime.putAll(absencesMap)
            return dateToOvertime
        }

    /**
     * returns the dates and corresponding absence durations for the given date
     */
    private val absencesMap: Map<LocalDate, Duration>
        get() {
            val overtimeAbsencesSince = workingtimeItemProvider
                    .overtimeAbsences
            val resultMap = TreeMap<LocalDate, Duration>()
            for ((key, value) in overtimeAbsencesSince) {
                resultMap[key] = value.min
            }
            return resultMap
        }

    /**
     * @return overtime information from the given time to the other given time
     */
    fun getOvertime(from: LocalDate, to: LocalDate): Map<LocalDate, Duration> {
        val result = TreeMap<LocalDate, Duration>()

        for ((key, value) in overtime) {
            if (DateTimes.isBetween(key, from, to)) {
                result[key] = value
            }
        }
        return result
    }

    /**
     * returns the overtime (positive or negative) for the given date and
     * duration
     */
    private fun getOvertime(date: LocalDate, duration: Duration): Duration {
        val workingTimeForDate = workingtimeItemProvider
                .getWorkingTimeFor(date)
        if (duration.compareTo(workingTimeForDate.max) > 0) {
            return duration.minus(workingTimeForDate.max)
        } else if (duration.compareTo(workingTimeForDate.min) < 0) {
            return duration.minus(workingTimeForDate.min)
        }

        return Duration.ZERO
    }
}
