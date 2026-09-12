package org.stt

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class IntRangeTest {
    @Test
    fun shouldStoreStartAndEnd() {
        // GIVEN

        // WHEN
        val sut = IntRange(5, 10)

        // THEN
        assertThat(sut.start).isEqualTo(5)
        assertThat(sut.end).isEqualTo(10)
    }

    @Test
    fun shouldAllowStartEqualToEnd() {
        // GIVEN

        // WHEN
        val sut = IntRange(7, 7)

        // THEN
        assertThat(sut.start).isEqualTo(sut.end)
    }

    @Test
    fun shouldAllowStartGreaterThanEnd() {
        // GIVEN

        // WHEN
        val sut = IntRange(10, 5)

        // THEN
        assertThat(sut.start).isGreaterThan(sut.end)
    }
}