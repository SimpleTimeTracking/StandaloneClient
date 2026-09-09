package org.stt.submit.csv

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.stt.model.TimeTrackingItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tests for [PerDayDurations].
 *
 * Verifies per-day duration splitting: same-day items, items spanning midnight (including ends
 * exactly at midnight), multi-day items with partial first/last days, seconds preservation, and
 * zero-length items producing no days.
 */
class PerDayDurationsTest {

    private val jan1 = LocalDate.of(2020, 1, 1)
    private val jan2 = LocalDate.of(2020, 1, 2)
    private val jan3 = LocalDate.of(2020, 1, 3)

    @Test
    fun shouldReturnEmptyMapForZeroDurationItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 10, 0))

        // WHEN
        val result = PerDayDurations.split(item)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun shouldReturnSingleDayForSameDayItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 30))

        // WHEN
        val result = PerDayDurations.split(item)

        // THEN
        assertThat(result).containsExactlyEntriesOf(mapOf(jan1 to Duration.ofMinutes(90)))
    }

    @Test
    fun shouldSplitItemSpanningMidnightIntoTwoDays() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 22, 0), LocalDateTime.of(2020, 1, 2, 2, 0))

        // WHEN
        val result = PerDayDurations.split(item)

        // THEN
        assertThat(result).containsExactlyEntriesOf(mapOf(
            jan1 to Duration.ofHours(2),
            jan2 to Duration.ofHours(2)))
    }

    @Test
    fun shouldSplitItemEndingExactlyAtMidnightToSingleDay() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 22, 0), LocalDateTime.of(2020, 1, 2, 0, 0))

        // WHEN
        val result = PerDayDurations.split(item)

        // THEN
        assertThat(result).containsExactlyEntriesOf(mapOf(jan1 to Duration.ofHours(2)))
    }

    @Test
    fun shouldSplitMultiDayItemIntoPerDayPortions() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 3, 10, 0))

        // WHEN
        val result = PerDayDurations.split(item)

        // THEN
        assertThat(result).containsExactlyEntriesOf(mapOf(
            jan1 to Duration.ofHours(14),
            jan2 to Duration.ofHours(24),
            jan3 to Duration.ofHours(10)))
    }

    @Test
    fun shouldSplitPartialDaysForMultiDayItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 23, 0), LocalDateTime.of(2020, 1, 4, 1, 0))

        // WHEN
        val result = PerDayDurations.split(item)

        // THEN
        assertThat(result).containsExactlyEntriesOf(mapOf(
            jan1 to Duration.ofHours(1),
            jan2 to Duration.ofHours(24),
            jan3 to Duration.ofHours(24),
            LocalDate.of(2020, 1, 4) to Duration.ofHours(1)))
    }

    @Test
    fun shouldSplitPreservingSeconds() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 22, 0, 30), LocalDateTime.of(2020, 1, 2, 2, 0, 30))

        // WHEN
        val result = PerDayDurations.split(item)

        // THEN
        assertThat(result).containsExactlyEntriesOf(mapOf(
            jan1 to Duration.ofMinutes(119).plusSeconds(30),
            jan2 to Duration.ofMinutes(120).plusSeconds(30)))
    }
}
