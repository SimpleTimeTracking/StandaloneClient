package org.stt.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class DateTimesTest {
    @Test
    fun shouldReturnTrueForSameDate() {
        // GIVEN

        // WHEN
        val a = LocalDateTime.now().withHour(10)
        val b = a.plusHours(2)
        val result = DateTimes.isOnSameDay(a, b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseForDifferentDates() {
        // GIVEN
        val a = LocalDateTime.of(2024, 1, 1, 10, 0)
        val b = LocalDateTime.of(2024, 1, 2, 10, 0)

        // WHEN
        val result = DateTimes.isOnSameDay(a, b)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnFalseWhenFirstIsNull() {
        // GIVEN
        val b = LocalDateTime.now()

        // WHEN
        val result = DateTimes.isOnSameDay(null, b)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnFalseWhenSecondIsNull() {
        // GIVEN
        val a = LocalDateTime.now()

        // WHEN
        val result = DateTimes.isOnSameDay(a, null)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueForToday() {
        // GIVEN
        val now = LocalDateTime.now()

        // WHEN
        val result = DateTimes.isToday(now)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseForNonToday() {
        // GIVEN
        val yesterday = LocalDateTime.now().minusDays(1)

        // WHEN
        val result = DateTimes.isToday(yesterday)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueForTodayDate() {
        // GIVEN
        val today = LocalDate.now()

        // WHEN
        val result = DateTimes.isToday(today)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseForNonTodayDate() {
        // GIVEN
        val yesterday = LocalDate.now().minusDays(1)

        // WHEN
        val result = DateTimes.isToday(yesterday)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueWhenSourceIsBetweenFromAndTo() {
        // GIVEN
        val from = LocalDate.of(2024, 1, 1)
        val to = LocalDate.of(2024, 1, 31)
        val source = LocalDate.of(2024, 1, 15)

        // WHEN
        val result = DateTimes.isBetween(source, from, to)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueWhenSourceEqualsFrom() {
        // GIVEN
        val from = LocalDate.of(2024, 1, 1)
        val to = LocalDate.of(2024, 1, 31)
        val source = LocalDate.of(2024, 1, 1)

        // WHEN
        val result = DateTimes.isBetween(source, from, to)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueWhenSourceEqualsTo() {
        // GIVEN
        val from = LocalDate.of(2024, 1, 1)
        val to = LocalDate.of(2024, 1, 31)
        val source = LocalDate.of(2024, 1, 31)

        // WHEN
        val result = DateTimes.isBetween(source, from, to)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseWhenSourceIsBeforeFrom() {
        // GIVEN
        val from = LocalDate.of(2024, 1, 1)
        val to = LocalDate.of(2024, 1, 31)
        val source = LocalDate.of(2023, 12, 31)

        // WHEN
        val result = DateTimes.isBetween(source, from, to)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnFalseWhenSourceIsAfterTo() {
        // GIVEN
        val from = LocalDate.of(2024, 1, 1)
        val to = LocalDate.of(2024, 1, 31)
        val source = LocalDate.of(2024, 2, 1)

        // WHEN
        val result = DateTimes.isBetween(source, from, to)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun prettyPrintTimeShouldUseTimeOnlyForToday() {
        // GIVEN
        val now = LocalDateTime.now()

        // WHEN
        val result = DateTimes.prettyPrintTime(now)

        // THEN
        assertThat(result).matches("\\d{2}:\\d{2}:\\d{2}")
    }

    @Test
    fun prettyPrintTimeShouldUseFullFormatForNonToday() {
        // GIVEN
        val date = LocalDateTime.of(2023, 6, 15, 10, 30, 0)

        // WHEN
        val result = DateTimes.prettyPrintTime(date)

        // THEN
        assertThat(result).isEqualTo("2023.06.15 10:30:00")
    }

    @Test
    fun prettyPrintTimeShouldReturnBeginningOfTimeForMinDate() {
        // GIVEN
        val date = LocalDateTime.of(LocalDate.MIN, java.time.LocalTime.MIN)

        // WHEN
        val result = DateTimes.prettyPrintTime(date)

        // THEN
        assertThat(result).isEqualTo("beginning of time")
    }

    @Test
    fun prettyPrintDateShouldReturnBeginningOfTimeForMinDate() {
        // GIVEN

        // WHEN
        val result = DateTimes.prettyPrintDate(LocalDate.MIN)

        // THEN
        assertThat(result).isEqualTo("beginning of time")
    }

    @Test
    fun prettyPrintDateShouldReturnFormattedDate() {
        // GIVEN
        val date = LocalDate.of(2024, 3, 15)

        // WHEN
        val result = DateTimes.prettyPrintDate(date)

        // THEN
        assertThat(result).isEqualTo("2024-03-15")
    }

    @Test
    fun prettyPrintDurationShouldFormatPositiveDuration() {
        // GIVEN
        val duration = Duration.ofHours(2).plusMinutes(30).plusSeconds(15)

        // WHEN
        val result = DateTimes.prettyPrintDuration(duration)

        // THEN
        assertThat(result).isEqualTo(" 2:30:15")
    }

    @Test
    fun prettyPrintDurationShouldFormatZeroDuration() {
        // GIVEN
        val duration = Duration.ZERO

        // WHEN
        val result = DateTimes.prettyPrintDuration(duration)

        // THEN
        assertThat(result).isEqualTo(" 0:00:00")
    }

    @Test
    fun prettyPrintDurationShouldFormatNegativeDuration() {
        // GIVEN
        val duration = Duration.ofMinutes(-45)

        // WHEN
        val result = DateTimes.prettyPrintDuration(duration)

        // THEN
        assertThat(result).isEqualTo("-0:45:00")
    }
}