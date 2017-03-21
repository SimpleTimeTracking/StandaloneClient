package org.stt.query;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.time.Interval;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(Theories.class)
public class TimeTrackingItemQueriesTest {

    @Mock
    private ItemReader reader;
    private TimeTrackingItemQueries sut;

    private static LocalDateTime BASE = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
    private static final LocalDateTime _1500 = BASE.plus(1500, SECONDS);
    private static final LocalDateTime _1200 = BASE.plus(1200, SECONDS);
    private static final LocalDateTime _1000 = BASE.plus(1000, SECONDS);
    private static final LocalDateTime _999 = BASE.plus(999, SECONDS);
    private static final LocalDateTime _800 = BASE.plus(800, SECONDS);
    private static final LocalDateTime _500 = BASE.plus(500, SECONDS);
    private static final LocalDateTime _100 = BASE.plus(100, SECONDS);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sut = new TimeTrackingItemQueries(() -> reader, Optional.empty());
    }

    private TimeTrackingItem[] givenOneTTIPerHourStartingWith(LocalDateTime start, int amount) {
        TimeTrackingItem[] items = new TimeTrackingItem[amount];
        LocalDateTime lastTime = start;
        for (int i = 0; i < items.length; i++) {
            LocalDateTime next = lastTime.plusHours(1);
            items[i] = new TimeTrackingItem("", lastTime, next);
            lastTime = next;
        }
        givenReaderReturns(items);
        return items;
    }

    @Test
    public void shouldNotFindCurrentItemIfNoneCanBeRead() {
        // GIVEN
        given(reader.read()).willReturn(Optional.empty());

        // WHEN
        Optional<TimeTrackingItem> result = sut.getOngoingItem();

        // THEN
        assertThat(result, is(Optional.<TimeTrackingItem>empty()));
    }

    @Test
    public void shouldFindCurrentItem() {
        // GIVEN
        TimeTrackingItem unfinishedItem = new TimeTrackingItem("",
                LocalDateTime.now());
        given(reader.read()).willReturn(Optional.of(unfinishedItem))
                .willReturn(Optional.empty());

        // WHEN
        Optional<TimeTrackingItem> result = sut.getOngoingItem();

        // THEN
        assertThat(result.get(), is(unfinishedItem));
    }

    @Test
    public void shouldNotFindCurrentItemIfLastOneIsFinished() {
        // GIVEN
        TimeTrackingItem unfinishedItem = new TimeTrackingItem("",
                LocalDateTime.now(), LocalDateTime.now().plus(1, MILLIS));
        given(reader.read()).willReturn(Optional.of(unfinishedItem))
                .willReturn(Optional.empty());

        // WHEN
        Optional<TimeTrackingItem> result = sut.getOngoingItem();

        // THEN
        assertThat(result, is(Optional.<TimeTrackingItem>empty()));
    }

    @Test
    public void shouldOnlyFindCurrentItem() {
        // GIVEN
        TimeTrackingItem finishedItem = new TimeTrackingItem("",
                LocalDateTime.now(), LocalDateTime.now().plus(1, MILLIS));
        TimeTrackingItem unfinishedItem = new TimeTrackingItem("",
                LocalDateTime.now());
        givenReaderReturns(finishedItem, unfinishedItem);

        // WHEN
        Optional<TimeTrackingItem> result = sut.getOngoingItem();

        // THEN
        assertThat(result.get(), is(unfinishedItem));
    }

    private void givenReaderReturns(TimeTrackingItem... items) {
        ItemReaderTestHelper.givenReaderReturns(reader, items);
    }

    @Test
    public void allTrackedDaysShouldNotReturnSameDateTimeTwice() {
        // GIVEN
        LocalDateTime[] dateTimes = {LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                LocalDateTime.of(2000, 1, 1, 0, 0, 0)};

        givenReaderReturnsTrackingTimesForStartDates(dateTimes);

        // WHEN
        Collection<LocalDate> result = sut.queryAllTrackedDays().collect(toList());

        // THEN
        LocalDate last = null;
        for (LocalDate current : result) {
            assertThat(last, anyOf(nullValue(), not(is(current))));
            last = current;
        }
    }

    @Theory
    public void allTrackedDaysShouldReturnADayPerDay(@TestedOn(ints = {0, 1,
            3, 10}) int days) {
        // GIVEN
        Collection<LocalDateTime> timesForItems = new ArrayList<>();
        LocalDateTime timeForItem = LocalDateTime.of(2000, 1, 1, 3, 2, 7);
        for (int i = 0; i < days; i++) {
            timesForItems.add(timeForItem);
            timeForItem = timeForItem.plusDays(1);
        }
        givenReaderReturnsTrackingTimesForStartDates(timesForItems
                .toArray(new LocalDateTime[timesForItems.size()]));

        // WHEN
        Collection<LocalDate> result = sut.queryAllTrackedDays().collect(toList());

        // THEN
        assertThat(result, IsCollectionWithSize.hasSize(days));
        Iterator<LocalDate> resultIt = result.iterator();
        Iterator<LocalDateTime> timesForItemsIt = timesForItems.iterator();
        while (resultIt.hasNext() || timesForItemsIt.hasNext()) {
            LocalDate trackedDay = resultIt.next();
            LocalDateTime trackedItem = timesForItemsIt.next();
            assertThat(trackedDay, is(trackedItem.toLocalDate()));
        }
    }

    private void givenReaderReturnsTrackingTimesForStartDates(
            LocalDateTime[] dateTimes) {
        TimeTrackingItem[] items = new TimeTrackingItem[dateTimes.length];
        for (int i = 0; i < dateTimes.length; i++) {
            items[i] = new TimeTrackingItem("", dateTimes[i]);
        }
        ItemReaderTestHelper.givenReaderReturns(reader, items);
    }

    @Test
    public void shouldReturnItemsWithinInterval() {
        // GIVEN
        Interval queryInterval = Interval.between(_500, _1000);
        givenReaderReturnsTrackingTimesForStartDates(new LocalDateTime[]{_100, _500, _1000, _1500});

        Criteria criteria = new Criteria();
        criteria.withStartBetween(queryInterval);
        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(criteria).collect(toList());

        // THEN
        assertThat(mapItemToStartDateTime(result), Matchers.is(Arrays.asList(new LocalDateTime[]{_500})));
    }

    @Test
    public void shouldReturnItemsWithStartBefore() {
        // GIVEN
        givenReaderReturnsTrackingTimesForStartDates(new LocalDateTime[]{_100, _500, _1000, _1500});

        Criteria criteria = new Criteria();
        criteria.withStartBefore(_500);

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(criteria).collect(toList());

        // THEN
        assertThat(mapItemToStartDateTime(result), Matchers.is(Arrays.asList(new LocalDateTime[]{_100})));
    }

    @Test
    public void shouldReturnItemsWithStartNotBefore() {
        // GIVEN
        givenReaderReturnsTrackingTimesForStartDates(new LocalDateTime[]{_100, _500, _1000, _1500});

        Criteria criteria = new Criteria();
        criteria.withStartNotBefore(_1000);

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(criteria).collect(toList());

        // THEN
        assertThat(mapItemToStartDateTime(result), Matchers.is(Arrays.asList(new LocalDateTime[]{_1000, _1500})));
    }

    @Test
    public void shouldReturnItemWithEndNotAfter() {
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem("", _800, _1000);
        givenReaderReturns(expectedResult, new TimeTrackingItem("", _1000, _1200));
        Criteria Criteria = new Criteria();
        Criteria.withEndNotAfter(_1000);

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(Criteria).collect(toList());

        // THEN
        assertThat(result, CoreMatchers.is(Collections.singletonList(expectedResult)));
    }

    @Test
    public void shouldReturnItemWithEndBefore() {
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem("", _800, _999);
        givenReaderReturns(expectedResult, new TimeTrackingItem("", _800, _1000));
        Criteria criteria = new Criteria();
        criteria.withEndBefore(_1000);

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(criteria).collect(toList());

        // THEN
        assertThat(result, CoreMatchers.is(Collections.singletonList(expectedResult)));
    }

    @Test
    public void shouldReturnItemOnDay() {
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem("", LocalDateTime.of(2015, 1, 3, 1, 1), LocalDateTime.of(2015, 1, 3, 3, 3));
        givenReaderReturns(expectedResult, new TimeTrackingItem("", _800, _1000));
        Criteria criteria = new Criteria();
        criteria.withPeriodAtDay(LocalDate.of(2015, 1, 3));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(criteria).collect(toList());


        // THEN
        assertThat(result, CoreMatchers.is(Collections.singletonList(expectedResult)));
    }


    private Collection<LocalDateTime> mapItemToStartDateTime(Collection<TimeTrackingItem> items) {
        return items.stream()
                .map(TimeTrackingItem::getStart)
                .collect(toList());
    }

    @Test
    public void shouldFilterUsingQuery() throws IOException {
        // GIVEN
        LocalDateTime from = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2000, 1, 2, 0, 0);
        givenReaderReturns(new TimeTrackingItem("", from, to));
        Criteria criteria = new Criteria();
        criteria.withStartBetween(Interval.between(from, to));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryAllItems().collect(toList());

        // THEN
        assertThat(result, IsCollectionWithSize.hasSize(1));
        assertThat(result.iterator().next().getStart(), is(from));
    }

    @Test
    public void itemContainingSubstringGetsFound() throws IOException {

        // GIVEN
        givenReaderReturns(new TimeTrackingItem("the comment", LocalDateTime.now()));

        Criteria criteria = new Criteria();
        criteria.withCommentContains("comment");

        // WHEN
        Optional<TimeTrackingItem> read = sut.queryItems(criteria).findFirst();

        // THEN
        Assert.assertEquals("the comment", read.get().getActivity());
    }

    @Test
    public void itemNotContainingSubstringGetsNotFound() throws IOException {

        // GIVEN
        givenReaderReturns(new TimeTrackingItem("the comment", LocalDateTime.now()));

        Criteria criteria = new Criteria();
        criteria.withCommentContains("not there");

        // WHEN
        Optional<TimeTrackingItem> read = sut.queryItems(criteria).findFirst();

        // THEN
        Assert.assertEquals(Optional.<TimeTrackingItem>empty(), read);
    }

    @Test
    public void emptyCriteriaReturnsAllItems() throws IOException {

        // GIVEN
        givenReaderReturns(new TimeTrackingItem("the comment", LocalDateTime.now()));

        Criteria criteria = new Criteria();

        // WHEN
        Optional<TimeTrackingItem> read = sut.queryItems(criteria).findFirst();

        // THEN
        Assert.assertEquals("the comment", read.get().getActivity());
    }

    @Test
    public void cachedItemsCanBeReadMultipleTimes() {
        // GIVEN
        TimeTrackingItem expected1 = new TimeTrackingItem("first",
                LocalDateTime.now());
        TimeTrackingItem expected2 = new TimeTrackingItem("second",
                LocalDateTime.now());
        givenReaderReturns(expected1, expected2);

        // WHEN
        // read all items, so the original reader is definitely exhausted
        sut.queryAllItems().collect(toList());

        // THEN
        List<TimeTrackingItem> secondCall = sut.queryAllItems().collect(toList());
        Assert.assertThat(secondCall, IsCollectionWithSize.hasSize(2));
        Assert.assertThat(secondCall, hasItems(expected1, expected2));
    }

    @Test
    public void shouldNotFindPreviousNorNextItem() {
        // GIVEN
        TimeTrackingItem itemToSearch = new TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 2));
        givenReaderReturns(itemToSearch);

        // WHEN
        TimeTrackingItemQueries.AdjacentItems adjacentItems = sut.getAdjacentItems(itemToSearch);

        // THEN
        assertThat(adjacentItems.previousItem(), is(Optional.empty()));
    }

    @Test
    public void shouldFindPreviousItem() {
        // GIVEN
        TimeTrackingItem expected = new TimeTrackingItem("first",
                LocalDateTime.of(2000, 10, 10, 1, 1), LocalDateTime.of(2000, 10, 10, 1, 2));
        TimeTrackingItem itemToSearch = new TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 2));
        givenReaderReturns(expected, itemToSearch);

        // WHEN
        TimeTrackingItemQueries.AdjacentItems adjacentItems = sut.getAdjacentItems(itemToSearch);

        // THEN
        assertThat(adjacentItems.previousItem().get(), is(expected));
    }

    @Test
    public void shouldFindNextItem() {
        // GIVEN
        TimeTrackingItem itemToSearch = new TimeTrackingItem("first",
                LocalDateTime.of(2000, 10, 10, 1, 1), LocalDateTime.of(2000, 10, 10, 1, 2));
        TimeTrackingItem expected = new TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 2));
        givenReaderReturns(itemToSearch, expected);

        // WHEN
        TimeTrackingItemQueries.AdjacentItems adjacentItems = sut.getAdjacentItems(itemToSearch);

        // THEN
        assertThat(adjacentItems.nextItem().get(), is(expected));
    }

    @Test
    public void shouldFindPreviousAndNextItem() {
        // GIVEN
        TimeTrackingItem expectedPrevious = new TimeTrackingItem("first",
                LocalDateTime.of(2000, 10, 10, 1, 1), LocalDateTime.of(2000, 10, 10, 1, 2));
        TimeTrackingItem itemToSearch = new TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 2), LocalDateTime.of(2000, 10, 10, 1, 3));
        TimeTrackingItem expectedNext = new TimeTrackingItem("third",
                LocalDateTime.of(2000, 10, 10, 1, 3));
        givenReaderReturns(expectedPrevious, itemToSearch, expectedNext);

        // WHEN
        TimeTrackingItemQueries.AdjacentItems adjacentItems = sut.getAdjacentItems(itemToSearch);

        // THEN
        assertThat(adjacentItems.previousItem().get(), is(expectedPrevious));
        assertThat(adjacentItems.nextItem().get(), is(expectedNext));
    }

    @Test
    public void shouldNotFindPreviousNorNextItemForGaps() {
        // GIVEN
        TimeTrackingItem previousWithGap = new TimeTrackingItem("first",
                LocalDateTime.of(2000, 10, 10, 1, 1), LocalDateTime.of(2000, 10, 10, 1, 2));
        TimeTrackingItem itemToSearch = new TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 3), LocalDateTime.of(2000, 10, 10, 1, 4));
        TimeTrackingItem nextWithGap = new TimeTrackingItem("third",
                LocalDateTime.of(2000, 10, 10, 1, 5));
        givenReaderReturns(previousWithGap, itemToSearch, nextWithGap);

        // WHEN
        TimeTrackingItemQueries.AdjacentItems adjacentItems = sut.getAdjacentItems(itemToSearch);

        // THEN
        assertThat(adjacentItems.previousItem(), is(Optional.empty()));
        assertThat(adjacentItems.nextItem(), is(Optional.empty()));
    }
}
