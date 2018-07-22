package org.stt.command

import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.stt.Matchers.any
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemPersister
import org.stt.query.TimeTrackingItemQueries
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream

class ActivitiesTest {
    private var sut: Activities? = null
    @Mock
    private lateinit var persister: ItemPersister
    @Mock
    private lateinit var queries: TimeTrackingItemQueries

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = Activities(persister, queries, Optional.empty())
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
}