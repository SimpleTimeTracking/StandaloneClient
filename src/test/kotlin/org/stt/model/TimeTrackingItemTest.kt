package org.stt.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.stt.time.preciseToSecond
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class TimeTrackingItemTest {

    @Test
    fun shouldCreateItemWithoutEnd() {
        // GIVEN

        // WHEN
        val (_, _, end) = TimeTrackingItem("", now())

        // THEN
        assertThat(end).isNull()
    }

    @Test
    fun shouldCreateItemWithStartIfEndIsMissing() {
        // GIVEN

        // WHEN
        val (_, start) = TimeTrackingItem("", now())

        // THEN
        assertThat(start).isNotNull()
    }

    @Test
    fun shouldCreateItemWithStart() {
        // GIVEN

        // WHEN
        val (_, start) = TimeTrackingItem("", now(),
                now().plusMinutes(1))

        // THEN
        assertThat(start).isNotNull()
    }

    @Test
    fun withEndShouldCreateNewItem() {
        // GIVEN
        val sut = TimeTrackingItem("", now())

        // WHEN
        val newEndTime = now().preciseToSecond().plusMinutes(2)
        val newItem = sut.withEnd(newEndTime)

        // THEN
        assertThat(newItem).isNotEqualTo(sut)
        assertThat(sut.end).isNull()
        assertThat(newItem.end).isEqualTo(newEndTime)
    }

    @Test
    fun withStartShouldCreateNewItem() {
        // GIVEN
        val sut = TimeTrackingItem("", now())

        // WHEN
        val newStartTime = now().plusMinutes(2).preciseToSecond()
        val newItem = sut.withStart(newStartTime)

        // THEN
        assertThat(newItem).isNotEqualTo(sut)
        assertThat(newItem.start).isEqualTo(newStartTime)
    }

    @Test
    fun withPendingEndShouldCreateNewItem() {
        // GIVEN
        val sut = TimeTrackingItem("", now(), now().plusDays(1))

        // WHEN
        val newItem = sut.withPendingEnd()

        // THEN
        assertThat(newItem).isNotEqualTo(sut)
        assertThat(newItem.end).isNull()
    }

    @Test
    fun withActivityShouldCreateNewItem() {
        // GIVEN
        val sut = TimeTrackingItem("", now(), now().plusDays(1))

        // WHEN
        val newItem = sut.withActivity("11")

        // THEN
        assertThat(newItem).isNotEqualTo(sut)
        assertThat(newItem.activity).isEqualTo("11")
    }

    @Test
    fun withActivityShouldCreateNewItemForOngoing() {
        // GIVEN
        val sut = TimeTrackingItem("", now())

        // WHEN
        val newItem = sut.withActivity("11")

        // THEN
        assertThat(newItem).isNotEqualTo(sut)
        assertThat(newItem.activity).isEqualTo("11")
    }

    @Test
    fun shouldReturnTrueForSameStart() {
        // GIVEN
        val a = TimeTrackingItem("", now(), now().plusDays(1))
        val b = a.withEnd(now().plusDays(2))

        // WHEN
        val result = a.sameStartAs(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseForDifferentStart() {
        // GIVEN
        val a = TimeTrackingItem("", now(), now().plusDays(1))
        val b = a.withStart(now().plusSeconds(1))

        // WHEN
        val result = a.sameStartAs(b)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueForSameActivity() {
        // GIVEN
        val a = TimeTrackingItem("Activity", now(), now().plusDays(1))
        val b = a.withStart(now().plusSeconds(1))

        // WHEN
        val result = a.sameActivityAs(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseForDifferentActivity() {
        // GIVEN
        val a = TimeTrackingItem("Activity", now(), now().plusDays(1))
        val b = a.withStart(now().plusSeconds(1)).withActivity("Other")

        // WHEN
        val result = a.sameActivityAs(b)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueForSameEnd() {
        // GIVEN
        val a = TimeTrackingItem("", now(), now().plusDays(1))
        val b = a.withStart(now().plusSeconds(1))

        // WHEN
        val result = a.sameEndAs(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueForBothOngoing() {
        // GIVEN
        val a = TimeTrackingItem("", now())
        val b = a.withStart(now().plusSeconds(1))

        // WHEN
        val result = a.sameEndAs(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseForOnlyOneOngoing() {
        // GIVEN
        val a = TimeTrackingItem("", now())
        val b = a.withStart(now().plusSeconds(1)).withEnd(now().plusDays(1))

        // WHEN
        val result = a.sameEndAs(b)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnFalseForDifferentEnds() {
        // GIVEN
        val a = TimeTrackingItem("", now()).withEnd(now().plusDays(2))
        val b = a.withStart(now().plusSeconds(1)).withEnd(now().plusDays(1))

        // WHEN
        val result = a.sameEndAs(b)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueForPartialOverlap() {
        // GIVEN
        val a = TimeTrackingItem("", now()).withEnd(now().plusDays(2))
        val b = a.withStart(now().plusDays(1)).withEnd(now().plusDays(2))

        // WHEN
        val result = a.intersects(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueForEnclosingInterval() {
        // GIVEN
        val a = TimeTrackingItem("", now()).withEnd(now().plusDays(3))
        val b = a.withStart(now().plusDays(1)).withEnd(now().plusDays(1))

        // WHEN
        val result = a.intersects(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueForEqualInterval() {
        // GIVEN
        val a = TimeTrackingItem("", now()).withEnd(now().plusDays(3))

        // WHEN
        val result = a.intersects(a)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseForStartMatchingOtherEnd() {
        // GIVEN
        val end = now().plusDays(1)
        val a = TimeTrackingItem("", now()).withEnd(end)
        val b = a.withEnd(now().plusDays(2)).withStart(end)

        // WHEN
        val result = a.intersects(b)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueIfOngoingAndOtherIsNot() {
        // GIVEN
        val a = TimeTrackingItem("", now())
        val b = TimeTrackingItem("", now(), now().plusDays(1))

        // WHEN
        val result = a.endsSameOrAfter(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueIfBothAreOngoing() {
        // GIVEN
        val a = TimeTrackingItem("", now())
        val b = TimeTrackingItem("", now())

        // WHEN
        val result = a.endsSameOrAfter(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueIfEndsAfterOther() {
        // GIVEN
        val a = TimeTrackingItem("", now(), now().plusDays(2))
        val b = TimeTrackingItem("", now(), now().plusDays(1))

        // WHEN
        val result = a.endsSameOrAfter(b)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseIfEndsBeforeOther() {
        // GIVEN
        val a = TimeTrackingItem("", now(), now().plusDays(1))
        val b = TimeTrackingItem("", now(), now().plusDays(2))

        // WHEN
        val result = a.endsSameOrAfter(b)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueIfEndsAtSameTime() {
        // GIVEN
        val a = TimeTrackingItem("", now(), now().plusDays(1))

        // WHEN
        val result = a.endsSameOrAfter(a)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueIfEndsBeforeDate() {
        // GIVEN
        val a = TimeTrackingItem("", now(), now().plusDays(1))

        // WHEN
        val result = a.endsAtOrBefore(now().plusDays(2))

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueIfEndsAtDate() {
        // GIVEN
        val end = now().plusDays(1)
        val a = TimeTrackingItem("", now(), end)

        // WHEN
        val result = a.endsAtOrBefore(end)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseIfEndsAfter() {
        // GIVEN
        val a = TimeTrackingItem("", now(), now().plusDays(2))

        // WHEN
        val result = a.endsAtOrBefore(now().plusDays(1))

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnFalseIfOngoing() {
        // GIVEN
        val a = TimeTrackingItem("", now())

        // WHEN
        val result = a.endsAtOrBefore(now())

        // THEN
        assertThat(result).isFalse()
    }

}
