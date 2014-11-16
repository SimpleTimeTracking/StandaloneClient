package org.stt.time;

import static org.hamcrest.CoreMatchers.is;
import org.joda.time.Duration;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 * @author dante
 */
public class DurationRounderTest {

	private final DurationRounder sut = new DurationRounder();

	@Test
	public void shouldRound500msTo0msWith1SecondInterval() {
		// GIVEN
		sut.setInterval(Duration.standardSeconds(1));

		// WHEN
		Duration result = sut.roundDuration(Duration.millis(500));

		// THEN
		assertThat(result, is(Duration.ZERO));
	}

	@Test
	public void shouldRound1500msTo2000msWith1SecondInterval() {
		// GIVEN
		sut.setInterval(Duration.standardSeconds(1));

		// WHEN
		Duration result = sut.roundDuration(Duration.millis(1500));

		// THEN
		assertThat(result, is(Duration.standardSeconds(2)));
	}

	@Test
	public void shouldRound5minsTo7minsWith7MinsInterval() {
		// GIVEN
		sut.setInterval(Duration.standardMinutes(7));

		// WHEN
		Duration result = sut.roundDuration(Duration.standardMinutes(5));

		// THEN
		assertThat(result, is(Duration.standardMinutes(7)));
	}

	@Test
	public void shouldRound30minsTo0minsWith1HourInterval() {
		// GIVEN
		sut.setInterval(Duration.standardHours(1));

		// WHEN
		Duration result = sut.roundDuration(Duration.standardMinutes(30));

		// THEN
		assertThat(result, is(Duration.ZERO));
	}

	@Test
	public void shouldRound7HoursTo5HoursWith5HourInterval() {
		// GIVEN
		sut.setInterval(Duration.standardHours(5));

		// WHEN
		Duration result = sut.roundDuration(Duration.standardHours(7));

		// THEN
		assertThat(result, is(Duration.standardHours(5)));
	}
}
