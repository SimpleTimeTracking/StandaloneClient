package org.stt.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

public class TimeTrackingItemTest {

	@Test
	public void shouldCreateItemWithoutEnd() {
		// GIVEN

		// WHEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now());

		// THEN
		assertThat(sut.getEnd().isPresent(), is(false));
	}

	@Test
	public void shouldCreateItemWithStartIfEndIsMissing() {
		// GIVEN

		// WHEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now());

		// THEN
		assertThat(sut.getStart(), notNullValue());
	}

	@Test
	public void shouldCreateItemWithStart() {
		// GIVEN

		// WHEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now(),
				DateTime.now().plusMinutes(1));

		// THEN
		assertThat(sut.getStart(), notNullValue());
	}

	@Test
	public void withEndShouldCreateNewItem() {
		// GIVEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now());

		// WHEN
		DateTime newEndTime = DateTime.now().plusMinutes(2);
		TimeTrackingItem newItem = sut.withEnd(newEndTime);

		// THEN
		assertThat(sut.getEnd().isPresent(), is(false));
		assertThat(newItem.getEnd().get(), is(newEndTime));
	}
}
