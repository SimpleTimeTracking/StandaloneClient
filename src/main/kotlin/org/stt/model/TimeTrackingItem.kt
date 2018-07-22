package org.stt.model

import org.stt.States.requireThat
import org.stt.time.preciseToSecond
import java.time.LocalDateTime


class TimeTrackingItem(val activity: String, start: LocalDateTime, end: LocalDateTime? = null) {
    val start: LocalDateTime
    val end: LocalDateTime?

    init {
        requireThat(end?.isBefore(start) != true, "end must not be before start for item!")
        this.start = start.preciseToSecond()
        this.end = end?.preciseToSecond()
    }

    operator fun component1() = activity
    operator fun component2() = start
    operator fun component3() = end

    fun sameEndAs(other: TimeTrackingItem) = end == other.end

    fun sameActivityAs(other: TimeTrackingItem) = activity == other.activity

    fun sameStartAs(other: TimeTrackingItem) = start == other.start

    fun intersects(other: TimeTrackingItem) =
            (end?.isAfter(other.start) ?: true) && (other.end?.isAfter(start) ?: true)

    fun endsSameOrAfter(other: TimeTrackingItem) = other.end == end || other.end != null && end?.isBefore(other.end) != true

    fun endsAtOrBefore(dateTime: LocalDateTime) = end != null && !dateTime.isBefore(end)

    fun withEnd(newEnd: LocalDateTime): TimeTrackingItem = TimeTrackingItem(activity, start, newEnd)

    fun withPendingEnd(): TimeTrackingItem = TimeTrackingItem(activity, start)

    fun withStart(newStart: LocalDateTime): TimeTrackingItem = TimeTrackingItem(activity, newStart, end)

    fun withActivity(newActivity: String): TimeTrackingItem = TimeTrackingItem(newActivity, start, end)

    override fun toString(): String = ("$start - $end : $activity")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimeTrackingItem

        if (activity != other.activity) return false
        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activity.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + (end?.hashCode() ?: 0)
        return result
    }
}
