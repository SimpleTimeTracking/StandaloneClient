package org.stt.model

import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import org.stt.time.preciseToSecond
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class TimeTrackingItemTest {

    @Test
    fun shouldCreateItemWithoutEnd() {
        // GIVEN

        // WHEN
        val (_, _, end) = TimeTrackingItem("", LocalDateTime.now())

        // THEN
        assertThat<LocalDateTime>(end, `is`(nullValue()))
    }

    @Test
    fun shouldCreateItemWithStartIfEndIsMissing() {
        // GIVEN

        // WHEN
        val (_, start) = TimeTrackingItem("", LocalDateTime.now())

        // THEN
        assertThat(start, notNullValue())
    }

    @Test
    fun shouldCreateItemWithStart() {
        // GIVEN

        // WHEN
        val (_, start) = TimeTrackingItem("", LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(1))

        // THEN
        assertThat(start, notNullValue())
    }

    @Test
    fun withEndShouldCreateNewItem() {
        // GIVEN
        val sut = TimeTrackingItem("", LocalDateTime.now())

        // WHEN
        val newEndTime = now().preciseToSecond().plusMinutes(2)
        val newItem = sut.withEnd(newEndTime)

        // THEN
        assertThat(newItem, not(`is`(sut)))
        assertThat<LocalDateTime>(sut.end, `is`(nullValue()))
        assertThat<LocalDateTime>(newItem.end, `is`(newEndTime))
    }

    @Test
    fun withStartShouldCreateNewItem() {
        // GIVEN
        val sut = TimeTrackingItem("", LocalDateTime.now())

        // WHEN
        val newStartTime = now().plusMinutes(2).preciseToSecond()
        val newItem = sut.withStart(newStartTime)

        // THEN
        assertThat(newItem, not(`is`(sut)))
        assertThat(newItem.start, `is`(newStartTime))
    }

    @Test
    fun withPendingEndShouldCreateNewItem() {
        // GIVEN
        val sut = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))

        // WHEN
        val newItem = sut.withPendingEnd()

        // THEN
        assertThat(newItem, not(`is`(sut)))
        assertThat(newItem.end, `is`(nullValue()))
    }

    @Test
    fun withActivityShouldCreateNewItem() {
        // GIVEN
        val sut = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))

        // WHEN
        val newItem = sut.withActivity("11")

        // THEN
        assertThat(newItem, not(`is`(sut)))
        assertThat(newItem.activity, `is`("11"))
    }

    @Test
    fun withActivityShouldCreateNewItemForOngoing() {
        // GIVEN
        val sut = TimeTrackingItem("", LocalDateTime.now())

        // WHEN
        val newItem = sut.withActivity("11")

        // THEN
        assertThat(newItem, not(`is`(sut)))
        assertThat(newItem.activity, `is`("11"))
    }

    @Test
    fun shouldReturnTrueForSameStart() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))
        val b = a.withEnd(LocalDateTime.now().plusDays(2))

        // WHEN
        val result = a.sameStartAs(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnFalseForDifferentStart() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))
        val b = a.withStart(LocalDateTime.now().plusSeconds(1))

        // WHEN
        val result = a.sameStartAs(b)

        // THEN
        assertThat(result, `is`(false))
    }

    @Test
    fun shouldReturnTrueForSameActivity() {
        // GIVEN
        val a = TimeTrackingItem("Activity", LocalDateTime.now(), LocalDateTime.now().plusDays(1))
        val b = a.withStart(LocalDateTime.now().plusSeconds(1))

        // WHEN
        val result = a.sameActivityAs(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnFalseForDifferentActivity() {
        // GIVEN
        val a = TimeTrackingItem("Activity", LocalDateTime.now(), LocalDateTime.now().plusDays(1))
        val b = a.withStart(LocalDateTime.now().plusSeconds(1)).withActivity("Other")

        // WHEN
        val result = a.sameActivityAs(b)

        // THEN
        assertThat(result, `is`(false))
    }

    @Test
    fun shouldReturnTrueForSameEnd() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))
        val b = a.withStart(LocalDateTime.now().plusSeconds(1))

        // WHEN
        val result = a.sameEndAs(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnTrueForBothOngoing() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now())
        val b = a.withStart(LocalDateTime.now().plusSeconds(1))

        // WHEN
        val result = a.sameEndAs(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnFalseForOnlyOneOngoing() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now())
        val b = a.withStart(LocalDateTime.now().plusSeconds(1)).withEnd(LocalDateTime.now().plusDays(1))

        // WHEN
        val result = a.sameEndAs(b)

        // THEN
        assertThat(result, `is`(false))
    }

    @Test
    fun shouldReturnFalseForDifferentEnds() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now()).withEnd(LocalDateTime.now().plusDays(2))
        val b = a.withStart(LocalDateTime.now().plusSeconds(1)).withEnd(LocalDateTime.now().plusDays(1))

        // WHEN
        val result = a.sameEndAs(b)

        // THEN
        assertThat(result, `is`(false))
    }

    @Test
    fun shouldReturnTrueForPartialOverlap() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now()).withEnd(LocalDateTime.now().plusDays(2))
        val b = a.withStart(LocalDateTime.now().plusDays(1)).withEnd(LocalDateTime.now().plusDays(2))

        // WHEN
        val result = a.intersects(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnTrueForEnclosingInterval() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now()).withEnd(LocalDateTime.now().plusDays(3))
        val b = a.withStart(LocalDateTime.now().plusDays(1)).withEnd(LocalDateTime.now().plusDays(1))

        // WHEN
        val result = a.intersects(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnTrueForEqualInterval() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now()).withEnd(LocalDateTime.now().plusDays(3))

        // WHEN
        val result = a.intersects(a)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnFalseForStartMatchingOtherEnd() {
        // GIVEN
        val end = LocalDateTime.now().plusDays(1)
        val a = TimeTrackingItem("", LocalDateTime.now()).withEnd(end)
        val b = a.withEnd(LocalDateTime.now().plusDays(2)).withStart(end)

        // WHEN
        val result = a.intersects(b)

        // THEN
        assertThat(result, `is`(false))
    }

    @Test
    fun shouldReturnTrueIfOngoingAndOtherIsNot() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now())
        val b = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))

        // WHEN
        val result = a.endsSameOrAfter(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnTrueIfBothAreOngoing() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now())
        val b = TimeTrackingItem("", LocalDateTime.now())

        // WHEN
        val result = a.endsSameOrAfter(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnTrueIfEndsAfterOther() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(2))
        val b = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))

        // WHEN
        val result = a.endsSameOrAfter(b)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnFalseIfEndsBeforeOther() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))
        val b = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(2))

        // WHEN
        val result = a.endsSameOrAfter(b)

        // THEN
        assertThat(result, `is`(false))
    }

    @Test
    fun shouldReturnTrueIfEndsAtSameTime() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))

        // WHEN
        val result = a.endsSameOrAfter(a)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnTrueIfEndsBeforeDate() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(1))

        // WHEN
        val result = a.endsAtOrBefore(LocalDateTime.now().plusDays(2))

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnTrueIfEndsAtDate() {
        // GIVEN
        val end = LocalDateTime.now().plusDays(1)
        val a = TimeTrackingItem("", LocalDateTime.now(), end)

        // WHEN
        val result = a.endsAtOrBefore(end)

        // THEN
        assertThat(result, `is`(true))
    }

    @Test
    fun shouldReturnFalseIfEndsAfter() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now(), LocalDateTime.now().plusDays(2))

        // WHEN
        val result = a.endsAtOrBefore(LocalDateTime.now().plusDays(1))

        // THEN
        assertThat(result, `is`(false))
    }

    @Test
    fun shouldReturnFalseIfOngoing() {
        // GIVEN
        val a = TimeTrackingItem("", LocalDateTime.now())

        // WHEN
        val result = a.endsAtOrBefore(LocalDateTime.now())

        // THEN
        assertThat(result, `is`(false))
    }

}
