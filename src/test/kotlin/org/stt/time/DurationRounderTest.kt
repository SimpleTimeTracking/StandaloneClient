package org.stt.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration

/**
 *
 * @author dante
 */
class DurationRounderTest {

    private val sut = DurationRounder()

    @Test
    fun shouldRound500msTo0msWith1SecondInterval() {
        // GIVEN
        sut.setInterval(Duration.ofSeconds(1))

        // WHEN
        val result = sut.roundDuration(Duration.ofMillis(500))

        // THEN
        assertThat(result).isEqualTo(Duration.ZERO)
    }

    @Test
    fun shouldRound1500msTo2000msWith1SecondInterval() {
        // GIVEN
        sut.setInterval(Duration.ofSeconds(1))

        // WHEN
        val result = sut.roundDuration(Duration.ofMillis(1500))

        // THEN
        assertThat(result).isEqualTo(Duration.ofSeconds(2))
    }

    @Test
    fun shouldRound5minsTo7minsWith7MinsInterval() {
        // GIVEN
        sut.setInterval(Duration.ofMinutes(7))

        // WHEN
        val result = sut.roundDuration(Duration.ofMinutes(5))

        // THEN
        assertThat(result).isEqualTo(Duration.ofMinutes(7))
    }

    @Test
    fun shouldRound30minsTo0minsWith1HourInterval() {
        // GIVEN
        sut.setInterval(Duration.ofHours(1))

        // WHEN
        val result = sut.roundDuration(Duration.ofMinutes(30))

        // THEN
        assertThat(result).isEqualTo(Duration.ZERO)
    }

    @Test
    fun shouldRound7HoursTo5HoursWith5HourInterval() {
        // GIVEN
        sut.setInterval(Duration.ofHours(5))

        // WHEN
        val result = sut.roundDuration(Duration.ofHours(7))

        // THEN
        assertThat(result).isEqualTo(Duration.ofHours(5))
    }
}
