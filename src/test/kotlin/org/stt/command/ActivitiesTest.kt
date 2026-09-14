package org.stt.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.stt.Matchers.any
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemPersister
import org.stt.query.TimeTrackingItemQueries
import org.stt.submit.SubmitStatusTracker
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream

/**
 * Tests for [Activities].
 *
 * Besides the gap-closing and ongoing-item behaviors, verifies that all mutating command paths
 * (add, end, remove, remove-and-close-gap, bulk change) reject already-submitted items with an
 * [IllegalStateException] and leave persistence untouched, while resume operations stay allowed.
 */
class ActivitiesTest {
    private var sut: Activities? = null
    @Mock
    private lateinit var persister: ItemPersister
    @Mock
    private lateinit var queries: TimeTrackingItemQueries
    private val submitStatusTracker: SubmitStatusTracker = Mockito.mock(SubmitStatusTracker::class.java)

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = Activities(persister, queries, Optional.empty(), submitStatusTracker)
    }

    @Test
    fun shouldFillGapOnDeleteIfPreviousAndNextMatch() {
        // GIVEN
        val before = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        val after = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))
        val expected = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(before, after))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).persist(expected)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldNotFillGapOnDeleteIfPreviousAndNextDiffer() {
        // GIVEN
        val before = TimeTrackingItem("before",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        val after = TimeTrackingItem("after",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(before, after))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).delete(itemToDelete)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldFillGapOnDeletedIsOngoing() {
        // GIVEN
        val before = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val expected = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9))

        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(before, null))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).persist(expected)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldNotFillGapIfPreviousItemStartsOnDifferentDay() {
        // GIVEN
        val before = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 9, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10))

        given(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(before, null))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).delete(itemToDelete)
        verifyNoMoreInteractions(persister)
    }


    @Test
    fun shouldDeletedWithoutPrevious() {
        // GIVEN
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        val after = TimeTrackingItem("after",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(null, after))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).delete(itemToDelete)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldEndOngoingItemIfItemWithSameActivityAndStartIsGiven() {
        // GIVEN
        val ongoing = TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val expected = TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        val newActivity = NewActivity(expected)

        given(queries.queryItems(any())).willReturn(Stream.of(ongoing))
        given<TimeTrackingItem>(queries.lastItem).willReturn(ongoing)

        // WHEN
        sut!!.addNewActivity(newActivity)

        // THEN
        verify<ItemPersister>(persister).replace(ongoing, expected)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldNotEndOngoingItemIfItemWithDifferentActivityWithSameStartIsGiven() {
        // GIVEN
        val expected = TimeTrackingItem("different",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        val newActivity = NewActivity(expected)

        given(queries.queryItems(any())).willReturn(Stream.empty())
        given<TimeTrackingItem>(queries.lastItem).willReturn(expected)

        // WHEN
        sut!!.addNewActivity(newActivity)

        // THEN
        verify<ItemPersister>(persister).persist(expected)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldBlockAddNewActivityForSubmittedItem() {
        // GIVEN
        val submitted = TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val newItem = TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 13))
        given(queries.queryItems(any())).willReturn(Stream.empty())
        given<TimeTrackingItem>(queries.lastItem).willReturn(submitted)
        doThrow(IllegalStateException("already submitted")).`when`(submitStatusTracker).requireNotSubmitted(submitted)

        // WHEN
        val result = kotlin.runCatching { sut!!.addNewActivity(NewActivity(newItem)) }

        // THEN
        assertThat(result.isFailure).isTrue()
        verifyNoInteractions(persister)
    }

    @Test
    fun shouldBlockEndCurrentActivityForSubmittedOngoingItem() {
        // GIVEN
        val submitted = TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 10))
        given<TimeTrackingItem>(queries.ongoingItem).willReturn(submitted)
        doThrow(IllegalStateException("already submitted")).`when`(submitStatusTracker).requireNotSubmitted(submitted)

        // WHEN
        val result = kotlin.runCatching { sut!!.endCurrentActivity(EndCurrentItem(LocalDateTime.of(2000, 10, 10, 11, 0))) }

        // THEN
        assertThat(result.isFailure).isTrue()
        verifyNoInteractions(persister)
    }

    @Test
    fun shouldBlockRemoveActivityForSubmittedItem() {
        // GIVEN
        val submitted = TimeTrackingItem("submitted",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        doThrow(IllegalStateException("already submitted")).`when`(submitStatusTracker).requireNotSubmitted(submitted)

        // WHEN
        val result = kotlin.runCatching { sut!!.removeActivity(RemoveActivity(submitted)) }

        // THEN
        assertThat(result.isFailure).isTrue()
        verifyNoInteractions(persister)
    }

    @Test
    fun shouldBlockRemoveActivityAndCloseGapIfAdjacentItemIsSubmitted() {
        // GIVEN
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        val submittedPrevious = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete))
                .willReturn(TimeTrackingItemQueries.AdjacentItems(submittedPrevious, null))
        doThrow(IllegalStateException("already submitted")).`when`(submitStatusTracker).requireNotSubmitted(submittedPrevious)

        // WHEN
        val result = kotlin.runCatching { sut!!.removeActivityAndCloseGap(RemoveActivity(itemToDelete)) }

        // THEN
        assertThat(result.isFailure).isTrue()
        verifyNoInteractions(persister)
    }

    @Test
    fun shouldBlockBulkChangeIfAnyItemIsSubmitted() {
        // GIVEN
        val submitted = TimeTrackingItem("submitted",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        val unsubmitted = TimeTrackingItem("unsubmitted",
                LocalDateTime.of(2000, 10, 10, 11, 0),
                LocalDateTime.of(2000, 10, 10, 12, 0))
        doThrow(IllegalStateException("already submitted")).`when`(submitStatusTracker).requireNotSubmitted(submitted)

        // WHEN
        val result = kotlin.runCatching { sut!!.bulkChangeActivity(listOf(unsubmitted, submitted), "renamed") }

        // THEN
        assertThat(result.isFailure).isTrue()
        verifyNoInteractions(persister)
    }

    @Test
    fun shouldAllowResumeForSubmittedItem() {
        // GIVEN
        val submitted = TimeTrackingItem("submitted",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))

        // WHEN
        sut!!.resumeActivity(ResumeActivity(submitted, LocalDateTime.of(2000, 10, 10, 12, 0)))

        // THEN
        verify<ItemPersister>(persister).persist(
                TimeTrackingItem("submitted", LocalDateTime.of(2000, 10, 10, 12, 0)))
        verifyNoMoreInteractions(persister)
    }
}