package org.stt.time;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DateTimesTest {
    @Test
    public void shouldReturnTrueForSameDate() {
        // GIVEN

        // WHEN
        LocalDateTime a = LocalDateTime.now().withHour(10);
        LocalDateTime b = a.plusHours(2);
        boolean result = DateTimes.isOnSameDay(a, b);

        // THEN
        assertThat(result, is(true));
    }

}