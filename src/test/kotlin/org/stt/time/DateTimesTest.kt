package org.stt.time

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
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
        assertThat(result, `is`(true))
    }

}