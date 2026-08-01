package org.stt.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class IntervalTest {
    @Test
    fun shouldStoreStartAndEnd() {
        // GIVEN
        val start = LocalDateTime.of(2024, 1, 1, 8, 0)
        val end = LocalDateTime.of(2024, 1, 1, 17, 0)

        // WHEN
        val sut = Interval(start, end)

        // THEN
        assertThat(sut.start).isEqualTo(start)
        assertThat(sut.end).isEqualTo(end)
    }

    @Test
    fun withEndShouldReturnNewIntervalWithUpdatedEnd() {
        // GIVEN
        val sut = Interval(
                LocalDateTime.of(2024, 1, 1, 8, 0),
                LocalDateTime.of(2024, 1, 1, 17, 0)
        )
        val newEnd = LocalDateTime.of(2024, 1, 1, 18, 0)

        // WHEN
        val result = sut.withEnd(newEnd)

        // THEN
        assertThat(result.start).isEqualTo(sut.start)
        assertThat(result.end).isEqualTo(newEnd)
    }

    @Test
    fun withEndShouldNotMutateOriginalInterval() {
        // GIVEN
        val start = LocalDateTime.of(2024, 1, 1, 8, 0)
        val end = LocalDateTime.of(2024, 1, 1, 17, 0)
        val sut = Interval(start, end)

        // WHEN
        sut.withEnd(LocalDateTime.of(2024, 1, 1, 18, 0))

        // THEN
        assertThat(sut.end).isEqualTo(end)
    }
}

class LocalDateAsIntervalTest {
    @Test
    fun asIntervalShouldCreateIntervalFromStartOfDayToStartOfNextDay() {
        // GIVEN
        val date = LocalDate.of(2024, 6, 15)

        // WHEN
        val result = date.asInterval()

        // THEN
        assertThat(result.start).isEqualTo(LocalDateTime.of(2024, 6, 15, 0, 0))
        assertThat(result.end).isEqualTo(LocalDateTime.of(2024, 6, 16, 0, 0))
    }
}

class LocalDateUntilExtensionTest {
    @Test
    fun untilShouldCreateIntervalFromStartOfDayToDayAfterEnd() {
        // GIVEN
        val from = LocalDate.of(2024, 1, 1)
        val to = LocalDate.of(2024, 1, 5)

        // WHEN
        val result = from until to

        // THEN
        assertThat(result.start).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0))
        assertThat(result.end).isEqualTo(LocalDateTime.of(2024, 1, 6, 0, 0))
    }

    @Test
    fun untilShouldCreateSingleDayIntervalWhenFromEqualsTo() {
        // GIVEN
        val from = LocalDate.of(2024, 6, 15)
        val to = LocalDate.of(2024, 6, 15)

        // WHEN
        val result = from until to

        // THEN
        assertThat(result.start).isEqualTo(LocalDateTime.of(2024, 6, 15, 0, 0))
        assertThat(result.end).isEqualTo(LocalDateTime.of(2024, 6, 16, 0, 0))
    }
}

class LocalDateTimeUntilExtensionTest {
    @Test
    fun untilShouldCreateIntervalBetweenTwoDateTimes() {
        // GIVEN
        val from = LocalDateTime.of(2024, 3, 10, 8, 30, 0)
        val to = LocalDateTime.of(2024, 3, 10, 17, 45, 0)

        // WHEN
        val result = from until to

        // THEN
        assertThat(result.start).isEqualTo(from)
        assertThat(result.end).isEqualTo(to)
    }
}