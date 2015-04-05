package org.stt.query;

import com.google.common.base.Optional;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(Theories.class)
public class DefaultTimeTrackingItemQueriesTest {

    @Mock
    private ItemReader reader;
    private ItemReaderProvider provider;
    private TimeTrackingItemQueries sut;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        provider = new ItemReaderProvider() {

            @Override
            public ItemReader provideReader() {
                return reader;
            }
        };
        sut = new DefaultTimeTrackingItemQueries(provider);
    }

    @Test
    public void shouldFindFirstItemsWithinInterval() {
        // GIVEN
        DateTime startOfRequest = new DateTime(2000, 1, 1, 1, 0);
        DateTime end = new DateTime(2000, 1, 1, 9, 1);
        TimeTrackingItem[] timeTrackingItems = givenOneTTIPerHourStartingWith(new DateTime(2000, 1, 1, 0, 0), 10);

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryFirstNItems(Optional.of(startOfRequest), Optional.of(end), Optional.of(2));

        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Arrays.asList(timeTrackingItems[1], timeTrackingItems[2])));
    }

    @Test
    public void shouldFindAllItemsWithinInterval() {
        // GIVEN
        DateTime startOfRequest = new DateTime(2000, 1, 1, 1, 0);
        DateTime end = new DateTime(2000, 1, 1, 5, 1);
        TimeTrackingItem[] timeTrackingItems = givenOneTTIPerHourStartingWith(new DateTime(2000, 1, 1, 0, 0), 6);

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryFirstNItems(Optional.of(startOfRequest), Optional.of(end), Optional.<Integer>absent());

        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Arrays.asList(timeTrackingItems[1], timeTrackingItems[2], timeTrackingItems[3],
                timeTrackingItems[4])));
    }

    private TimeTrackingItem[] givenOneTTIPerHourStartingWith(DateTime start, int amount) {
        TimeTrackingItem[] items = new TimeTrackingItem[amount];
        DateTime lastTime = start;
        for (int i = 0; i < items.length; i++) {
            DateTime next = lastTime.plusHours(1);
            items[i] = new TimeTrackingItem("", lastTime, next);
            lastTime = next;
        }
        givenReaderReturns(items);
        return items;
    }

    @Test
    public void shouldNotFindCurrentItemIfNoneCanBeRead() {
        // GIVEN
        given(reader.read()).willReturn(Optional.<TimeTrackingItem>absent());

        // WHEN
        Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

        // THEN
        assertThat(result, is(Optional.<TimeTrackingItem>absent()));
    }

    @Test
    public void shouldFindCurrentItem() {
        // GIVEN
        TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
                DateTime.now());
        given(reader.read()).willReturn(Optional.of(unfinishedItem))
                .willReturn(Optional.<TimeTrackingItem>absent());

        // WHEN
        Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

        // THEN
        assertThat(result.get(), is(unfinishedItem));
    }

    @Test
    public void shouldNotFindCurrentItemIfLastOneIsFinished() {
        // GIVEN
        TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
                DateTime.now(), DateTime.now().plusMillis(1));
        given(reader.read()).willReturn(Optional.of(unfinishedItem))
                .willReturn(Optional.<TimeTrackingItem>absent());

        // WHEN
        Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

        // THEN
        assertThat(result, is(Optional.<TimeTrackingItem>absent()));
    }

    @Test
    public void shouldOnlyFindCurrentItem() {
        // GIVEN
        TimeTrackingItem finishedItem = new TimeTrackingItem(null,
                DateTime.now(), DateTime.now().plusMillis(1));
        TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
                DateTime.now());
        givenReaderReturns(finishedItem, unfinishedItem);

        // WHEN
        Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

        // THEN
        assertThat(result.get(), is(unfinishedItem));
    }

    private void givenReaderReturns(TimeTrackingItem... items) {

        BDDMockito.BDDMyOngoingStubbing<Optional<TimeTrackingItem>> ongoingStubbing = given(reader.read());
        for (TimeTrackingItem item : items) {
            ongoingStubbing = ongoingStubbing.willReturn(Optional.of(item));
        }
        ongoingStubbing.willReturn(Optional.<TimeTrackingItem>absent());
    }

    @Test
    public void allTrackedDaysShouldNotReturnSameDateTimeTwice() {
        // GIVEN
        DateTime dateTimes[] = {new DateTime(2000, 1, 1, 0, 0, 0),
                new DateTime(2000, 1, 1, 0, 0, 0)};

        givenReaderReturnsTrackingTimesForStartDates(dateTimes);

        // WHEN
        Collection<DateTime> result = sut.getAllTrackedDays();

        // THEN
        DateTime last = null;
        for (DateTime current : result) {
            assertThat(last, anyOf(nullValue(), not(is(current))));
            last = current;
        }
    }

    @Test
    public void allTrackedDaysShouldBeAtStartOfDay() {
        // GIVEN
        DateTime dateTimes[] = {new DateTime(2000, 1, 1, 3, 2, 7),
                new DateTime(2010, 1, 1, 11, 12, 13)};

        givenReaderReturnsTrackingTimesForStartDates(dateTimes);

        // WHEN
        Collection<DateTime> result = sut.getAllTrackedDays();

        // THEN
        for (DateTime time : result) {
            assertThat(time, is(time.withTimeAtStartOfDay()));
        }
    }

    @Theory
    public void allTrackedDaysShouldReturnADayPerDay(@TestedOn(ints = {0, 1,
            3, 10}) int days) {
        // GIVEN
        Collection<DateTime> timesForItems = new ArrayList<>();
        DateTime timeForItem = new DateTime(2000, 1, 1, 3, 2, 7);
        for (int i = 0; i < days; i++) {
            timesForItems.add(timeForItem);
            timeForItem = timeForItem.plusDays(1);
        }
        givenReaderReturnsTrackingTimesForStartDates(timesForItems
                .toArray(new DateTime[timesForItems.size()]));

        // WHEN
        Collection<DateTime> result = sut.getAllTrackedDays();

        // THEN
        assertThat(result, IsCollectionWithSize.hasSize(days));
        Iterator<DateTime> resultIt = result.iterator();
        Iterator<DateTime> timesForItemsIt = timesForItems.iterator();
        while (resultIt.hasNext() || timesForItemsIt.hasNext()) {
            DateTime trackedDay = resultIt.next();
            DateTime trackedItem = timesForItemsIt.next();
            assertThat(trackedDay, is(trackedItem.withTimeAtStartOfDay()));
        }
    }

    private void givenReaderReturnsTrackingTimesForStartDates(
            DateTime[] dateTimes) {
        TimeTrackingItem[] items = new TimeTrackingItem[dateTimes.length];
        for (int i = 0; i < dateTimes.length; i++) {
            items[i] = new TimeTrackingItem(null, dateTimes[i]);
        }
        ItemReaderTestHelper.givenReaderReturns(reader, items);
    }

    @Test
    public void shouldReturnItemsWithinInterval() {
        // GIVEN
        Interval queryInterval = new Interval(500, 1000);
        givenReaderReturnsTrackingTimesForStartDates(new DateTime[]{new DateTime(100), new DateTime(500), new DateTime(1000), new DateTime(1500)});

        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartBetween(queryInterval);
        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);

        // THEN
        assertThat(mapItemToStartDateTime(result), Matchers.<Collection<DateTime>>is(Arrays.asList(new DateTime[]{new DateTime(500)})));
    }

    @Test
    public void shouldReturnItemsWithStartBefore() {
        // GIVEN
        givenReaderReturnsTrackingTimesForStartDates(new DateTime[]{new DateTime(100), new DateTime(500), new DateTime(1000), new DateTime(1500)});

        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartBefore(new DateTime(500));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);

        // THEN
        assertThat(mapItemToStartDateTime(result), Matchers.<Collection<DateTime>>is(Arrays.asList(new DateTime[]{new DateTime(100)})));
    }

    @Test
    public void shouldReturnItemsWithStartNotBefore() {
        // GIVEN
        givenReaderReturnsTrackingTimesForStartDates(new DateTime[]{new DateTime(100), new DateTime(500), new DateTime(1000), new DateTime(1500)});

        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartNotBefore(new DateTime(1000));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);

        // THEN
        assertThat(mapItemToStartDateTime(result), Matchers.<Collection<DateTime>>is(Arrays.asList(new DateTime[]{new DateTime(1000), new DateTime(1500)})));
    }

    @Test
    public void shouldReturnItemWithEndNotAfter() {
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem(null, new DateTime(800), new DateTime(1000));
        givenReaderReturns(expectedResult, new TimeTrackingItem(null, new DateTime(1000), new DateTime(1200)));
        DNFClause DNFClause = new DNFClause();
        DNFClause.withEndNotAfter(new DateTime(1000));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(DNFClause);

        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Collections.singletonList(expectedResult)));
    }

    @Test
    public void shouldReturnItemWithEndBefore() {
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem(null, new DateTime(800), new DateTime(999));
        givenReaderReturns(expectedResult, new TimeTrackingItem(null, new DateTime(800), new DateTime(1000)));
        DNFClause dnfClause = new DNFClause();
        dnfClause.withEndBefore(new DateTime(1000));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);

        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Collections.singletonList(expectedResult)));
    }

    @Test
    public void shouldReturnItemOnDay() {
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem(null, new DateTime(2015, 1, 3, 1, 1), new DateTime(2015, 1, 3, 3, 3));
        givenReaderReturns(expectedResult, new TimeTrackingItem(null, new DateTime(800), new DateTime(1000)));
        DNFClause dnfClause = new DNFClause();
        dnfClause.withPeriodAtDay(new LocalDate(2015, 1, 3));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);


        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Collections.singletonList(expectedResult)));
    }


    private Collection<DateTime> mapItemToStartDateTime(Collection<TimeTrackingItem> items) {
        ArrayList<DateTime> result = new ArrayList<>();
        for (TimeTrackingItem item: items) {
            result.add(item.getStart());
        }
        return result;
    }
}
