package org.stt.time

import java.time.LocalDate
import java.time.LocalDateTime

class Interval internal constructor(val start: LocalDateTime, val end: LocalDateTime) {
    fun withEnd(newEnd: LocalDateTime): Interval {
        return Interval(start, newEnd)
    }
}


fun LocalDate.asInterval() = Interval(atStartOfDay(), plusDays(1).atStartOfDay())
infix fun LocalDate.until(end: LocalDate) = Interval(atStartOfDay(), end.plusDays(1).atStartOfDay())
infix fun LocalDateTime.until(end: LocalDateTime) = Interval(this, end)