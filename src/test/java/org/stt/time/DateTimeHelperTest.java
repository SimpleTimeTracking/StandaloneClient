package org.stt.time;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DateTimeHelperTest {
    @Test
    public void shouldReturnTrueForSameDate() {
        // GIVEN

        // WHEN
        DateTime a = new DateTime().withHourOfDay(10);
        DateTime b = a.plusHours(2);
        boolean result = DateTimeHelper.isOnSameDay(a, b);

        // THEN
        assertThat(result, is(true));
    }

}