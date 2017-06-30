package org.stt.model;

import org.junit.Test;
import org.stt.time.DateTimes;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.time.LocalDateTime.now;
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
        LocalDateTime newEndTime = DateTimes.preciseToSecond(now().plusMinutes(2));
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
        LocalDateTime newStartTime = DateTimes.preciseToSecond(now().plusMinutes(2));
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

    @Test
    public void withActivityShouldCreateNewItem() {
        // GIVEN
        TimeTrackingItem sut = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // WHEN
        TimeTrackingItem newItem = sut.withActivity("11");

        // THEN
        assertThat(newItem, not(is(sut)));
        assertThat(newItem.getActivity(), is("11"));
    }

    @Test
    public void withActivityShouldCreateNewItemForOngoing() {
        // GIVEN
        TimeTrackingItem sut = new TimeTrackingItem("", LocalDateTime.now());

        // WHEN
        TimeTrackingItem newItem = sut.withActivity("11");

        // THEN
        assertThat(newItem, not(is(sut)));
        assertThat(newItem.getActivity(), is("11"));
    }

    @Test
    public void shouldReturnTrueForSameStart() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        TimeTrackingItem b = a.withEnd(LocalDateTime.now().plusDays(2));

        // WHEN
        boolean result = a.sameStartAs(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnFalseForDifferentStart() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusSeconds(1));

        // WHEN
        boolean result = a.sameStartAs(b);

        // THEN
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnTrueForSameActivity() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("Activity", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusSeconds(1));

        // WHEN
        boolean result = a.sameActivityAs(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnFalseForDifferentActivity() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("Activity", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusSeconds(1)).withActivity("Other");

        // WHEN
        boolean result = a.sameActivityAs(b);

        // THEN
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnTrueForSameEnd() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusSeconds(1));

        // WHEN
        boolean result = a.sameEndAs(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueForBothOngoing() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now());
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusSeconds(1));

        // WHEN
        boolean result = a.sameEndAs(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnFalseForOnlyOneOngoing() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now());
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusSeconds(1)).withEnd(LocalDateTime.now().plusDays(1));

        // WHEN
        boolean result = a.sameEndAs(b);

        // THEN
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnFalseForDifferentEnds() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now()).withEnd(LocalDateTime.now().plusDays(2));
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusSeconds(1)).withEnd(LocalDateTime.now().plusDays(1));

        // WHEN
        boolean result = a.sameEndAs(b);

        // THEN
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnTrueForPartialOverlap() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now()).withEnd(LocalDateTime.now().plusDays(2));
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusDays(1)).withEnd(LocalDateTime.now().plusDays(2));

        // WHEN
        boolean result = a.intersects(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueForEnclosingInterval() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now()).withEnd(LocalDateTime.now().plusDays(3));
        TimeTrackingItem b = a.withStart(LocalDateTime.now().plusDays(1)).withEnd(LocalDateTime.now().plusDays(1));

        // WHEN
        boolean result = a.intersects(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueForEqualInterval() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now()).withEnd(LocalDateTime.now().plusDays(3));

        // WHEN
        boolean result = a.intersects(a);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnFalseForStartMatchingOtherEnd() {
        // GIVEN
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now()).withEnd(end);
        TimeTrackingItem b = a.withEnd(LocalDateTime.now().plusDays(2)).withStart(end);

        // WHEN
        boolean result = a.intersects(b);

        // THEN
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnTrueIfOngoingAndOtherIsNot() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now());
        TimeTrackingItem b = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // WHEN
        boolean result = a.endsSameOrAfter(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueIfBothAreOngoing() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now());
        TimeTrackingItem b = new TimeTrackingItem("", LocalDateTime.now());

        // WHEN
        boolean result = a.endsSameOrAfter(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueIfEndsAfterOther() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        TimeTrackingItem b = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // WHEN
        boolean result = a.endsSameOrAfter(b);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnFalseIfEndsBeforeOther() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        TimeTrackingItem b = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(2));

        // WHEN
        boolean result = a.endsSameOrAfter(b);

        // THEN
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnTrueIfEndsAtSameTime() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // WHEN
        boolean result = a.endsSameOrAfter(a);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueIfEndsBeforeDate() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // WHEN
        boolean result = a.endsAtOrBefore(LocalDateTime.now().plusDays(2));

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueIfEndsAtDate() {
        // GIVEN
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), end);

        // WHEN
        boolean result = a.endsAtOrBefore(end);

        // THEN
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnFalseIfEndsAfter() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(2));

        // WHEN
        boolean result = a.endsAtOrBefore(LocalDateTime.now().plusDays(1));

        // THEN
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnFalseIfOngoing() {
        // GIVEN
        TimeTrackingItem a = new TimeTrackingItem("", LocalDateTime.now());

        // WHEN
        boolean result = a.endsAtOrBefore(LocalDateTime.now());

        // THEN
        assertThat(result, is(false));
    }

}
