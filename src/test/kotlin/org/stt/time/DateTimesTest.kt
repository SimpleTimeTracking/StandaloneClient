package org.stt.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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

}