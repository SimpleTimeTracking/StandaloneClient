package org.stt.command;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.query.Criteria;
import org.stt.query.TimeTrackingItemQueries;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ActivitiesTest {
    private Activities sut;
    @Mock
    private ItemPersister persister;
    @Mock
    private TimeTrackingItemQueries queries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sut = new Activities(persister, queries, Optional.empty());
    }

    @Test
    public void shouldFillGapOnDeleteIfPreviousAndNextMatch() {
        // GIVEN
        TimeTrackingItem before = new TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10));
        TimeTrackingItem itemToDelete = new TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12));
        TimeTrackingItem after = new TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13));
        TimeTrackingItem expected = new TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 13));

        given(queries.getAdjacentItems(itemToDelete)).willReturn(new TimeTrackingItemQueries.AdjacentItems(before, after));

        RemoveActivity removeActivity = new RemoveActivity(itemToDelete);

        // WHEN
        sut.removeActivityAndCloseGap(removeActivity);

        // THEN
        verify(persister).persist(expected);
        verifyNoMoreInteractions(persister);
    }

    @Test
    public void shouldNotFillGapOnDeleteIfPreviousAndNextDiffer() {
        // GIVEN
        TimeTrackingItem before = new TimeTrackingItem("before",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10));
        TimeTrackingItem itemToDelete = new TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12));
        TimeTrackingItem after = new TimeTrackingItem("after",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13));

        given(queries.getAdjacentItems(itemToDelete)).willReturn(new TimeTrackingItemQueries.AdjacentItems(before, after));

        RemoveActivity removeActivity = new RemoveActivity(itemToDelete);

        // WHEN
        sut.removeActivityAndCloseGap(removeActivity);

        // THEN
        verify(persister).delete(itemToDelete);
        verifyNoMoreInteractions(persister);
    }

    @Test
    public void shouldFillGapOnDeletedIsOngoing() {
        // GIVEN
        TimeTrackingItem before = new TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10));
        TimeTrackingItem itemToDelete = new TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10));
        TimeTrackingItem expected = new TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9));

        given(queries.getAdjacentItems(itemToDelete)).willReturn(new TimeTrackingItemQueries.AdjacentItems(before, null));

        RemoveActivity removeActivity = new RemoveActivity(itemToDelete);

        // WHEN
        sut.removeActivityAndCloseGap(removeActivity);

        // THEN
        verify(persister).persist(expected);
        verifyNoMoreInteractions(persister);
    }

    @Test
    public void shouldDeletedWithoutPrevious() {
        // GIVEN
        TimeTrackingItem itemToDelete = new TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12));
        TimeTrackingItem after = new TimeTrackingItem("after",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13));

        given(queries.getAdjacentItems(itemToDelete)).willReturn(new TimeTrackingItemQueries.AdjacentItems(null, after));

        RemoveActivity removeActivity = new RemoveActivity(itemToDelete);

        // WHEN
        sut.removeActivityAndCloseGap(removeActivity);

        // THEN
        verify(persister).delete(itemToDelete);
        verifyNoMoreInteractions(persister);
    }

    @Test
    public void shouldEndOngoingItemIfItemWithSameActivityAndStartIsGiven() {
        // GIVEN
        TimeTrackingItem ongoing = new TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 10));
        TimeTrackingItem expected = new TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13));

        NewActivity newActivity = new NewActivity(expected);

        given(queries.queryItems(any(Criteria.class))).willReturn(Stream.of(ongoing));
        given(queries.getLastItem()).willReturn(Optional.of(ongoing));

        // WHEN
        sut.addNewActivity(newActivity);

        // THEN
        verify(persister).replace(ongoing, expected);
        verifyNoMoreInteractions(persister);
    }

    @Test
    public void shouldNotEndOngoingItemIfItemWithDifferentActivityWithSameStartIsGiven() {
        // GIVEN
        TimeTrackingItem expected = new TimeTrackingItem("different",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13));

        NewActivity newActivity = new NewActivity(expected);

        given(queries.queryItems(any(Criteria.class))).willReturn(Stream.empty());
        given(queries.getLastItem()).willReturn(Optional.of(expected));

        // WHEN
        sut.addNewActivity(newActivity);

        // THEN
        verify(persister).persist(expected);
        verifyNoMoreInteractions(persister);
    }
}