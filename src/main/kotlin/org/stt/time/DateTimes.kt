package org.stt.time

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimes {
    val DATE_TIME_FORMATTER_HH_MM_SS = DateTimeFormatter
            .ofPattern("HH:mm:ss")
    val DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS = DateTimeFormatter
            .ofPattern("yyyy.MM.dd HH:mm:ss")
    val DATE_TIME_FORMATTER_YYYY_MM_DD_DASHED = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")

    val FORMATTER_PERIOD_HHh_MMm_SSs = { duration: Duration ->
        val seconds = duration.seconds
        String.format("%d:%02d:%02d",
                seconds / 3600, seconds % 3600 / 60, seconds % 60)
    }

    val FORMATTER_PERIOD_H_M_S = { duration: Duration ->
        val seconds = duration.seconds
        String.format("%d:%d:%d",
                seconds / 3600, seconds % 3600 / 60, seconds % 60)
    }


    fun isToday(date: LocalDateTime): Boolean {
        return LocalDate.now() == date.toLocalDate()
    }

    fun isToday(date: LocalDate): Boolean {
        return LocalDate.now() == date
    }

    fun isOnSameDay(a: LocalDateTime?, b: LocalDateTime?): Boolean {
        return a != null && b != null && a.toLocalDate() == b.toLocalDate()
    }

    /**
     * @return if source is between from and to (both inclusive)
     */
    fun isBetween(source: LocalDate, from: LocalDate, to: LocalDate): Boolean {
        return !source.isBefore(from) && !source.isAfter(to)
    }

    /**
     * returns the formatted date in asNewItemCommandText "HH:mm:ss" if the given date is
     * today, "yyyy-MM-dd HH:mm:ss" if the given date is not today
     */
    fun prettyPrintTime(date: LocalDateTime): String {
        return when {
            isToday(date) -> DATE_TIME_FORMATTER_HH_MM_SS.format(date)
            LocalDate.MIN == date.toLocalDate() -> "beginning of time"
            else -> DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS.format(date)
        }
    }

    /**
     * returns the formatted date in asNewItemCommandText "yyyy-MM-dd"
     *
     */
    fun prettyPrintDate(date: LocalDate): String {
        return if (LocalDate.MIN == date) {
            "beginning of time"
        } else DATE_TIME_FORMATTER_YYYY_MM_DD_DASHED.format(date)
    }

    fun prettyPrintDuration(duration: Duration): String {
        return if (duration.isNegative) {
            // it is negative
            "-" + FORMATTER_PERIOD_HHh_MMm_SSs(duration.abs())
        } else {
            " " + FORMATTER_PERIOD_HHh_MMm_SSs(duration)
        }
    }
}

fun LocalDateTime.preciseToSecond() = withNano(0)
