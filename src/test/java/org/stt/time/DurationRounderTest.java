package org.stt.time;

import org.junit.Test;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author dante
 */
public class DurationRounderTest {

	private final DurationRounder sut = new DurationRounder();

	@Test
	public void shouldRound500msTo0msWith1SecondInterval() {
		// GIVEN
        sut.setInterval(Duration.ofSeconds(1));

		// WHEN
        Duration result = sut.roundDuration(Duration.ofMillis(500));

		// THEN
		assertThat(result, is(Duration.ZERO));
	}

	@Test
	public void shouldRound1500msTo2000msWith1SecondInterval() {
		// GIVEN
        sut.setInterval(Duration.ofSeconds(1));

		// WHEN
        Duration result = sut.roundDuration(Duration.ofMillis(1500));

		// THEN
        assertThat(result, is(Duration.ofSeconds(2)));
    }

	@Test
	public void shouldRound5minsTo7minsWith7MinsInterval() {
		// GIVEN
        sut.setInterval(Duration.ofMinutes(7));

		// WHEN
        Duration result = sut.roundDuration(Duration.ofMinutes(5));

		// THEN
        assertThat(result, is(Duration.ofMinutes(7)));
    }

	@Test
	public void shouldRound30minsTo0minsWith1HourInterval() {
		// GIVEN
        sut.setInterval(Duration.ofHours(1));

		// WHEN
        Duration result = sut.roundDuration(Duration.ofMinutes(30));

		// THEN
		assertThat(result, is(Duration.ZERO));
	}

	@Test
	public void shouldRound7HoursTo5HoursWith5HourInterval() {
		// GIVEN
        sut.setInterval(Duration.ofHours(5));

		// WHEN
        Duration result = sut.roundDuration(Duration.ofHours(7));

		// THEN
        assertThat(result, is(Duration.ofHours(5)));
    }
}
