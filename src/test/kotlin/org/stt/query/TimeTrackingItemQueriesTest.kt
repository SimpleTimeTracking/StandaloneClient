package org.stt.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.experimental.theories.suppliers.TestedOn
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.stt.ItemReaderTestHelper
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import org.stt.time.until
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*
import javax.inject.Provider
import kotlin.streams.toList

@RunWith(Theories::class)
class TimeTrackingItemQueriesTest {

    @Mock
    private lateinit var reader: ItemReader
    private lateinit var sut: TimeTrackingItemQueries

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = TimeTrackingItemQueries(Provider { reader }, Optional.empty())
    }

    @Test
    fun shouldNotFindCurrentItemIfNoneCanBeRead() {
        // GIVEN
        given<TimeTrackingItem>(reader.read()).willReturn(null)

        // WHEN
        val result = sut.ongoingItem

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun shouldFindCurrentItem() {
        // GIVEN
        val unfinishedItem = TimeTrackingItem("",
                LocalDateTime.now())
        given<TimeTrackingItem>(reader.read()).willReturn(unfinishedItem)
                .willReturn(null)

        // WHEN
        val result = sut.ongoingItem

        // THEN
        assertThat(result!!).isEqualTo(unfinishedItem)
    }

    @Test
    fun shouldNotFindCurrentItemIfLastOneIsFinished() {
        // GIVEN
        val unfinishedItem = TimeTrackingItem("",
                LocalDateTime.now(), LocalDateTime.now().plus(1, MILLIS))
        given<TimeTrackingItem>(reader.read()).willReturn(unfinishedItem)
                .willReturn(null)

        // WHEN
        val result = sut.ongoingItem

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun shouldOnlyFindCurrentItem() {
        // GIVEN
        val finishedItem = TimeTrackingItem("",
                LocalDateTime.now(), LocalDateTime.now().plus(1, MILLIS))
        val unfinishedItem = TimeTrackingItem("",
                LocalDateTime.now())
        givenReaderReturns(finishedItem, unfinishedItem)

        // WHEN
        val result = sut.ongoingItem

        // THEN
        assertThat(result!!).isEqualTo(unfinishedItem)
    }

    private fun givenReaderReturns(vararg items: TimeTrackingItem) {
        ItemReaderTestHelper.givenReaderReturns(reader, *items)
    }

    @Test
    fun allTrackedDaysShouldNotReturnSameDateTimeTwice() {
        // GIVEN
        val dateTimes = arrayOf(LocalDateTime.of(2000, 1, 1, 0, 0, 0), LocalDateTime.of(2000, 1, 1, 0, 0, 0))

        givenReaderReturnsTrackingTimesForStartDates(dateTimes)

        // WHEN
        val result = sut.queryAllTrackedDays().toList()

        // THEN
        var last: LocalDate? = null
        for (current in result) {
            assertThat(last).isIn(null, current)
            last = current
        }
    }

    @Theory
    fun allTrackedDaysShouldReturnADayPerDay(@TestedOn(ints = intArrayOf(0, 1, 3, 10))
                                             days: Int) {
        // GIVEN
        val timesForItems = ArrayList<LocalDateTime>()
        var timeForItem = LocalDateTime.of(2000, 1, 1, 3, 2, 7)
        for (i in 0 until days) {
            timesForItems.add(timeForItem)
            timeForItem = timeForItem.plusDays(1)
        }
        givenReaderReturnsTrackingTimesForStartDates(timesForItems
                .toTypedArray())

        // WHEN
        val result = sut.queryAllTrackedDays().toList()

        // THEN
        assertThat(result).hasSize(days)
        val resultIt = result.iterator()
        val timesForItemsIt = timesForItems.iterator()
        while (resultIt.hasNext() || timesForItemsIt.hasNext()) {
            val trackedDay = resultIt.next()
            val trackedItem = timesForItemsIt.next()
            assertThat(trackedDay).isEqualTo(trackedItem.toLocalDate())
        }
    }

    private fun givenReaderReturnsTrackingTimesForStartDates(
            dateTimes: Array<LocalDateTime>) {
        val items = Array(dateTimes.size) {
            TimeTrackingItem("", dateTimes[it])
        }
        ItemReaderTestHelper.givenReaderReturns(reader, *items)
    }

    @Test
    fun shouldReturnItemsWithinInterval() {
        // GIVEN
        val queryInterval = _500 until _1000
        givenReaderReturnsTrackingTimesForStartDates(arrayOf(_100, _500, _1000, _1500))

        val criteria = Criteria()
        criteria.withStartBetween(queryInterval)
        // WHEN
        val result = sut.queryItems(criteria).toList()

        // THEN
        assertThat(mapItemToStartDateTime(result)).isEqualTo(Arrays.asList(_500))
    }

    @Test
    fun shouldReturnItemsWithStartBefore() {
        // GIVEN
        givenReaderReturnsTrackingTimesForStartDates(arrayOf(_100, _500, _1000, _1500))

        val criteria = Criteria()
        criteria.withStartBefore(_500)

        // WHEN
        val result = sut.queryItems(criteria).toList()

        // THEN
        assertThat(mapItemToStartDateTime(result)).isEqualTo(Arrays.asList(_100))
    }

    @Test
    fun shouldReturnItemsWithStartNotBefore() {
        // GIVEN
        givenReaderReturnsTrackingTimesForStartDates(arrayOf(_100, _500, _1000, _1500))

        val criteria = Criteria()
        criteria.withStartNotBefore(_1000)

        // WHEN
        val result = sut.queryItems(criteria).toList()

        // THEN
        assertThat(mapItemToStartDateTime(result)).isEqualTo(listOf(_1000, _1500))
    }

    @Test
    fun shouldReturnItemWithEndNotAfter() {
        // GIVEN
        val expectedResult = TimeTrackingItem("", _800, _1000)
        givenReaderReturns(expectedResult, TimeTrackingItem("", _1000, _1200))
        val criteria = Criteria()
        criteria.withEndNotAfter(_1000)

        // WHEN
        val result = sut.queryItems(criteria).toList()

        // THEN
        assertThat(result).isEqualTo(listOf(expectedResult))
    }

    @Test
    fun shouldReturnItemWithEndBefore() {
        // GIVEN
        val expectedResult = TimeTrackingItem("", _800, _999)
        givenReaderReturns(expectedResult, TimeTrackingItem("", _800, _1000))
        val criteria = Criteria()
        criteria.withEndBefore(_1000)

        // WHEN
        val result = sut.queryItems(criteria).toList()

        // THEN
        assertThat(result).isEqualTo(listOf(expectedResult))
    }

    @Test
    fun shouldReturnItemOnDay() {
        // GIVEN
        val expectedResult = TimeTrackingItem("", LocalDateTime.of(2015, 1, 3, 1, 1), LocalDateTime.of(2015, 1, 3, 3, 3))
        givenReaderReturns(expectedResult, TimeTrackingItem("", _800, _1000))
        val criteria = Criteria()
        criteria.withPeriodAtDay(LocalDate.of(2015, 1, 3))

        // WHEN
        val result = sut.queryItems(criteria).toList()


        // THEN
        assertThat(result).isEqualTo(listOf(expectedResult))
    }


    private fun mapItemToStartDateTime(items: Collection<TimeTrackingItem>) =
            items.map { it.start }.toList()

    @Test
    fun shouldFilterUsingQuery() {
        // GIVEN
        val from = LocalDateTime.of(2000, 1, 1, 0, 0)
        val to = LocalDateTime.of(2000, 1, 2, 0, 0)
        givenReaderReturns(TimeTrackingItem("", from, to))
        val criteria = Criteria()
        criteria.withStartBetween(from until to)

        // WHEN
        val result = sut.queryAllItems().toList()

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result).first().extracting { it.start }.isEqualTo(from)
    }

    @Test
    fun itemContainingSubstringGetsFound() {

        // GIVEN
        givenReaderReturns(TimeTrackingItem("the comment", LocalDateTime.now()))

        val criteria = Criteria()
        criteria.withActivityContains("comment")

        // WHEN
        val read = sut.queryItems(criteria).findFirst()

        // THEN
        assertThat("the comment").isEqualTo(read.get().activity)
    }

    @Test
    fun itemNotContainingSubstringGetsNotFound() {
        // GIVEN
        givenReaderReturns(TimeTrackingItem("the comment", LocalDateTime.now()))

        val criteria = Criteria()
        criteria.withActivityContains("not there")

        // WHEN
        val read = sut.queryItems(criteria).findFirst()

        // THEN
        assertThat(Optional.empty<TimeTrackingItem>()).isEqualTo(read)
    }

    @Test
    fun emptyCriteriaReturnsAllItems() {

        // GIVEN
        givenReaderReturns(TimeTrackingItem("the comment", LocalDateTime.now()))

        val criteria = Criteria()

        // WHEN
        val read = sut.queryItems(criteria).findFirst()

        // THEN
        assertThat("the comment").isEqualTo(read.get().activity)
    }

    @Test
    fun cachedItemsCanBeReadMultipleTimes() {
        // GIVEN
        val expected1 = TimeTrackingItem("first",
                LocalDateTime.now())
        val expected2 = TimeTrackingItem("second",
                LocalDateTime.now())
        givenReaderReturns(expected1, expected2)

        // WHEN
        // read all items, so the original reader is definitely exhausted
        sut.queryAllItems().toList()

        // THEN
        val secondCall = sut.queryAllItems().toList()
        assertThat(secondCall).hasSize(2)
        assertThat(secondCall).contains(expected1, expected2)
    }

    @Test
    fun shouldNotFindPreviousNorNextItem() {
        // GIVEN
        val itemToSearch = TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 2))
        givenReaderReturns(itemToSearch)

        // WHEN
        val adjacentItems = sut.getAdjacentItems(itemToSearch)

        // THEN
        assertThat(adjacentItems.previousItem).isNull()
    }

    @Test
    fun shouldFindPreviousItem() {
        // GIVEN
        val expected = TimeTrackingItem("first",
                LocalDateTime.of(2000, 10, 10, 1, 1), LocalDateTime.of(2000, 10, 10, 1, 2))
        val itemToSearch = TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 2))
        givenReaderReturns(expected, itemToSearch)

        // WHEN
        val adjacentItems = sut.getAdjacentItems(itemToSearch)

        // THEN
        assertThat(adjacentItems.previousItem).isEqualTo(expected)
    }

    @Test
    fun shouldFindNextItem() {
        // GIVEN
        val itemToSearch = TimeTrackingItem("first",
                LocalDateTime.of(2000, 10, 10, 1, 1), LocalDateTime.of(2000, 10, 10, 1, 2))
        val expected = TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 2))
        givenReaderReturns(itemToSearch, expected)

        // WHEN
        val adjacentItems = sut.getAdjacentItems(itemToSearch)

        // THEN
        assertThat(adjacentItems.nextItem).isEqualTo(expected)
    }

    @Test
    fun shouldFindPreviousAndNextItem() {
        // GIVEN
        val expectedPrevious = TimeTrackingItem("first",
                LocalDateTime.of(2000, 10, 10, 1, 1), LocalDateTime.of(2000, 10, 10, 1, 2))
        val itemToSearch = TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 2), LocalDateTime.of(2000, 10, 10, 1, 3))
        val expectedNext = TimeTrackingItem("third",
                LocalDateTime.of(2000, 10, 10, 1, 3))
        givenReaderReturns(expectedPrevious, itemToSearch, expectedNext)

        // WHEN
        val adjacentItems = sut.getAdjacentItems(itemToSearch)

        // THEN
        assertThat(adjacentItems.previousItem).isEqualTo(expectedPrevious)
        assertThat(adjacentItems.nextItem).isEqualTo(expectedNext)
    }

    @Test
    fun shouldNotFindPreviousNorNextItemForGaps() {
        // GIVEN
        val previousWithGap = TimeTrackingItem("first",
                LocalDateTime.of(2000, 10, 10, 1, 1), LocalDateTime.of(2000, 10, 10, 1, 2))
        val itemToSearch = TimeTrackingItem("second",
                LocalDateTime.of(2000, 10, 10, 1, 3), LocalDateTime.of(2000, 10, 10, 1, 4))
        val nextWithGap = TimeTrackingItem("third",
                LocalDateTime.of(2000, 10, 10, 1, 5))
        givenReaderReturns(previousWithGap, itemToSearch, nextWithGap)

        // WHEN
        val adjacentItems = sut.getAdjacentItems(itemToSearch)

        // THEN
        assertThat(adjacentItems.previousItem).isNull()
        assertThat(adjacentItems.nextItem).isNull()
    }

    @Test
    fun shouldMatchActivityIsNotCriteria() {
        // GIVEN
        givenReaderReturns(TimeTrackingItem("to be filtered", LocalDateTime.now()),
                TimeTrackingItem("not to be filtered", LocalDateTime.now()))

        val criteria = Criteria()
        criteria.withActivityIsNot("to be filtered")

        // WHEN
        val read = sut.queryItems(criteria).findFirst()

        // THEN
        assertThat("not to be filtered").isEqualTo(read.map { it.activity }.get())
    }

    companion object {

        private val BASE = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
        private val _1500 = BASE.plus(1500, SECONDS)
        private val _1200 = BASE.plus(1200, SECONDS)
        private val _1000 = BASE.plus(1000, SECONDS)
        private val _999 = BASE.plus(999, SECONDS)
        private val _800 = BASE.plus(800, SECONDS)
        private val _500 = BASE.plus(500, SECONDS)
        private val _100 = BASE.plus(100, SECONDS)
    }
}
