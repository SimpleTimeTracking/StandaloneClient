package org.stt.persistence.stt

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.stt.model.TimeTrackingItem
import java.time.LocalDateTime

/**
 */
class STTItemConverterTest {
    private val sut = STTItemConverter()

    @Test(timeout = 10000)
    fun shouldReadFastEnough() {
        // GIVEN
        val lineToTest = "2017-05-08_18:15:49 2017-05-08_19:00:00 3333333333333333333333"

        // WHEN
        for (i in 0..9999999) {
            sut.lineToTimeTrackingItem(lineToTest + i)
        }

        // THEN
    }

    @Test(timeout = 10000)
    fun shouldWriteFastEnough() {
        // GIVEN
        val item = TimeTrackingItem("test \n activity", LocalDateTime.now(), LocalDateTime.now().plusDays(1))

        // WHEN
        for (i in 0..4999999) {
            sut.timeTrackingItemToLine(item)
        }

        // THEN
    }


    @Test
    fun shouldParseStartToEnd() {
        // GIVEN
        val lineToTest = "2017-05-08_18:15:49 2017-05-08_19:00:00 Some Activity"

        // WHEN
        val item = sut.lineToTimeTrackingItem(lineToTest)

        // THEN
        assertThat(item).isEqualTo(TimeTrackingItem("Some Activity",
                LocalDateTime.of(2017, 5, 8, 18, 15, 49),
                LocalDateTime.of(2017, 5, 8, 19, 0)))
    }

    @Test
    fun shouldParseStartWithOpenEnd() {
        // GIVEN
        val lineToTest = "2019-01-10_10:11:12 Some Activity"

        // WHEN
        val item = sut.lineToTimeTrackingItem(lineToTest)

        // THEN
        assertThat(item).isEqualTo(TimeTrackingItem("Some Activity", LDT_FOR_TEST))
    }

    @Test
    fun shouldParseMultilineActivity() {
        // GIVEN
        val lineToTest = "2019-01-10_10:11:12 Some\\nActivity"

        // WHEN
        val item = sut.lineToTimeTrackingItem(lineToTest)

        // THEN
        assertThat(item).isEqualTo(TimeTrackingItem("Some\nActivity", LDT_FOR_TEST))
    }

    @Test
    fun shouldIgnoreEscapedBackslash() {
        // GIVEN
        val lineToTest = "2019-01-10_10:11:12 Some\\\\nActivity"

        // WHEN
        val item = sut.lineToTimeTrackingItem(lineToTest)

        // THEN
        assertThat(item).isEqualTo(TimeTrackingItem("Some\\nActivity", LDT_FOR_TEST))
    }

    @Test
    fun shouldEscapeBackslash() {
        // GIVEN
        val activity = TimeTrackingItem("\\n", LDT_FOR_TEST)

        // WHEN
        val line = sut.timeTrackingItemToLine(activity)

        // THEN
        assertThat(line).isEqualTo("2019-01-10_10:11:12 \\\\n")
    }

    companion object {
        private val LDT_FOR_TEST = LocalDateTime.of(2019, 1, 10, 10, 11, 12)
    }


}