package org.stt.model;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class TimeTrackingItemTest {

	@Test
	public void shouldCreateItemWithoutEnd() {
		// GIVEN

		// WHEN
        TimeTrackingItem sut = new TimeTrackingItem("", LocalDateTime.now());

		// THEN
		assertThat(sut.getEnd().isPresent(), is(false));
	}

	@Test
	public void shouldCreateItemWithStartIfEndIsMissing() {
		// GIVEN

		// WHEN
        TimeTrackingItem sut = new TimeTrackingItem("", LocalDateTime.now());

		// THEN
		assertThat(sut.getStart(), notNullValue());
	}

	@Test
	public void shouldCreateItemWithStart() {
		// GIVEN

		// WHEN
        TimeTrackingItem sut = new TimeTrackingItem("", LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(1));

		// THEN
		assertThat(sut.getStart(), notNullValue());
	}

	@Test
	public void withEndShouldCreateNewItem() {
		// GIVEN
        TimeTrackingItem sut = new TimeTrackingItem("", LocalDateTime.now());

		// WHEN
        LocalDateTime newEndTime = LocalDateTime.now().plusMinutes(2).withNano(0);
        TimeTrackingItem newItem = sut.withEnd(newEndTime);

		// THEN
		assertThat(newItem, not(is(sut)));
		assertThat(sut.getEnd().isPresent(), is(false));
		assertThat(newItem.getEnd().get(), is(newEndTime));
	}

	@Test
    public void withStartShouldCreateNewItem() {
        // GIVEN
        TimeTrackingItem sut = new TimeTrackingItem("", LocalDateTime.now());

        // WHEN
        LocalDateTime newStartTime = LocalDateTime.now().plusMinutes(2).withNano(0);
        TimeTrackingItem newItem = sut.withStart(newStartTime);

        // THEN
        assertThat(newItem, not(is(sut)));
        assertThat(newItem.getStart(), is(newStartTime));
    }

    @Test
    public void withPendingEndShouldCreateNewItem() {
        // GIVEN
        TimeTrackingItem sut = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // WHEN
        TimeTrackingItem newItem = sut.withPendingEnd();

        // THEN
        assertThat(newItem, not(is(sut)));
        assertThat(newItem.getEnd(), is(Optional.empty()));
    }

}
