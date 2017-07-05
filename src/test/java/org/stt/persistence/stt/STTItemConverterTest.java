package org.stt.persistence.stt;

import org.junit.Test;
import org.stt.model.TimeTrackingItem;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class STTItemConverterTest {
    private STTItemConverter sut = new STTItemConverter();

    @Test(timeout = 10000)
    public void shouldPerformSufficiently() {
        // GIVEN
        String lineToTest = "2017-05-08_18:15:49 2017-05-08_19:00:00 3333333333333333333333";

        // WHEN
        for (int i = 0; i < 2000000; i++) {
            sut.lineToTimeTrackingItem(lineToTest + i);
        }

        // THEN
    }

    @Test
    public void shouldParseStartToEnd() {
        // GIVEN
        String lineToTest = "2017-05-08_18:15:49 2017-05-08_19:00:00 Some Activity";

        // WHEN
        TimeTrackingItem item = sut.lineToTimeTrackingItem(lineToTest);

        // THEN
        assertThat(item, is(new TimeTrackingItem("Some Activity",
                LocalDateTime.of(2017, 5, 8, 18, 15, 49),
                LocalDateTime.of(2017, 5, 8, 19, 0))));
    }

    @Test
    public void shouldParseStartWithOpenEnd() {
        // GIVEN
        String lineToTest = "2019-01-10_10:11:12 Some Activity";

        // WHEN
        TimeTrackingItem item = sut.lineToTimeTrackingItem(lineToTest);

        // THEN
        assertThat(item, is(new TimeTrackingItem("Some Activity",
                LocalDateTime.of(2019, 1, 10, 10, 11, 12))));
    }

    @Test
    public void shouldParseMultilineActivity() {
        // GIVEN
        String lineToTest = "2019-01-10_10:11:12 Some\\nActivity";

        // WHEN
        TimeTrackingItem item = sut.lineToTimeTrackingItem(lineToTest);

        // THEN
        assertThat(item, is(new TimeTrackingItem("Some\nActivity",
                LocalDateTime.of(2019, 1, 10, 10, 11, 12))));
    }

}