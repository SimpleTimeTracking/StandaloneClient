package org.stt.importer

import org.hamcrest.Matchers.*
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import org.stt.model.TimeTrackingItem
import org.stt.persistence.stt.STTItemPersister
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDateTime
import javax.inject.Provider

@RunWith(Theories::class)
class STTItemWriterTest {
    private lateinit var stringWriter: StringWriter
    private lateinit var sut: STTItemPersister

    @Before
    fun setUp() {
        stringWriter = StringWriter()

        sut = STTItemPersister(Provider { StringReader(stringWriter.toString()) }, Provider { stringWriter = StringWriter(); stringWriter })
    }

    @Test
    fun writeCommentSucceeds() {

        // GIVEN
        val theItem = TimeTrackingItem("the comment",
                LocalDateTime.now())

        // WHEN
        sut.persist(theItem)

        // THEN
        Assert.assertThat(stringWriter.toString(),
                containsString("the comment"))
    }

    @Test
    fun writeStartSucceeds() {

        // GIVEN
        val theTime = LocalDateTime.of(2011, 10, 12, 13, 14, 15)
        val theItem = TimeTrackingItem("", theTime)

        // WHEN
        sut.persist(theItem)

        // THEN
        Assert.assertThat(stringWriter.toString(),
                containsString("2011-10-12_13:14:15"))
    }

    @Test
    fun writeEndSucceeds() {

        // GIVEN
        val start = LocalDateTime.of(2011, 10, 12, 13, 14, 15)
        val end = LocalDateTime.of(2012, 10, 12, 13, 14, 15)

        val theItem = TimeTrackingItem("", start, end)

        // WHEN
        sut.persist(theItem)

        // THEN
        Assert.assertThat(stringWriter.toString(),
                containsString("2012-10-12_13:14:15"))
    }

    @Test
    fun writeCompleteEntrySucceeds() {

        // GIVEN
        val start = LocalDateTime.of(2011, 10, 12, 13, 14, 15)

        val end = LocalDateTime.of(2012, 10, 12, 13, 14, 15)
        val theItem = TimeTrackingItem("the comment", start,
                end)

        // WHEN
        sut.persist(theItem)

        // THEN
        Assert.assertThat(
                stringWriter.toString(),
                containsString("2011-10-12_13:14:15 2012-10-12_13:14:15 the comment"))
    }

    @Test
    fun writeMultiLineEntrySucceeds() {

        // GIVEN
        val theItem = TimeTrackingItem(
                "this is\r\n a multiline\r string\n with different separators",
                LocalDateTime.now())

        // WHEN
        sut.persist(theItem)

        // THEN
        Assert.assertThat(
                stringWriter.toString(),
                endsWith("this is\\n a multiline\\n string\\n with different separators$LINE_SEPARATOR"))

    }

    @Test
    fun itemsCanBeDeleted() {

        // GIVEN
        val theItem = TimeTrackingItem("testitem",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13))
        val theItem2 = TimeTrackingItem("testitem",
                LocalDateTime.of(2014, 10, 10, 11, 12, 13))
        sut.persist(theItem)
        sut.persist(theItem2)

        // when
        sut.delete(theItem2)

        // then
        Assert.assertThat(stringWriter.toString(),
                `is`("2011-10-10_11:12:13 2014-10-10_11:12:13 testitem$LINE_SEPARATOR"))
    }

    @Test
    fun itemCanBeReplaced() {

        // GIVEN
        val theItem = TimeTrackingItem("testitem",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13))
        val theItem2 = TimeTrackingItem("testitem",
                LocalDateTime.now())
        sut.persist(theItem2)

        // when
        sut.replace(theItem2, theItem)

        // then
        Assert.assertThat(stringWriter.toString(),
                `is`("2011-10-10_11:12:13 testitem$LINE_SEPARATOR"))
    }

    @Test
    fun shouldWriteItemsWithMultipleWhitespaces() {
        // GIVEN
        val theItem = TimeTrackingItem("item with 2  spaces",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13))

        // when
        sut.persist(theItem)

        // then
        Assert.assertThat(stringWriter.toString(),
                `is`("2011-10-10_11:12:13 item with 2  spaces$LINE_SEPARATOR"))
    }

    @Theory
    fun shouldRemoveCoveredTimeIntervalsIfNewItemHasNoEnd(
            startOfNewItem: LocalDateTime) {
        val startOfExistingItem = LocalDateTime.of(2011, 10, 10, 11, 12, 13)

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem))
        // GIVEN
        val existingItem = TimeTrackingItem("testitem",
                startOfExistingItem)
        sut.persist(existingItem)

        val newItem = TimeTrackingItem("testitem2",
                startOfExistingItem)

        // WHEN
        sut.persist(newItem)

        // THEN
        Assert.assertThat(stringWriter.toString(),
                `is`("2011-10-10_11:12:13 testitem2$LINE_SEPARATOR"))
    }

    @Test
    fun shouldSetEndTimeIfNewItemIsStarted() {
        // GIVEN
        val existingItem = TimeTrackingItem("testitem",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13))
        sut.persist(existingItem)

        val newItem = TimeTrackingItem("testitem2",
                LocalDateTime.of(2011, 10, 10, 11, 12, 14))

        // WHEN
        sut.persist(newItem)

        // THEN
        Assert.assertThat(stringWriter.toString(),
                `is`("2011-10-10_11:12:13 2011-10-10_11:12:14 testitem"
                        + LINE_SEPARATOR + "2011-10-10_11:12:14 testitem2"
                        + LINE_SEPARATOR))

    }

    @Theory
    fun shouldRemoveCoveredTimeIntervalsIfCoveredByNewItem(
            startOfNewItem: LocalDateTime) {
        val startOfExistingItem = LocalDateTime.of(2011, 10, 10, 11, 12, 13)
        val endOfNewItem = LocalDateTime.of(2020, 10, 10, 11, 12, 13)

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem))
        // GIVEN
        val existingItem = TimeTrackingItem("existing item",
                startOfExistingItem, endOfNewItem)
        sut.persist(existingItem)

        val newItem = TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem)

        // WHEN
        sut.persist(newItem)

        // THEN
        Assert.assertThat(stringWriter.toString(),
                `is`("2011-10-10_11:12:13 2020-10-10_11:12:13 new item$LINE_SEPARATOR"))
    }

    @Theory
    fun shouldSplitOverlappingTimeIntervalWithEndIfNewItemEndsBefore(
            startOfNewItem: LocalDateTime) {
        val startOfExistingItem = LocalDateTime.of(2011, 10, 10, 11, 12, 13)
        val endOfNewItem = LocalDateTime.of(2020, 10, 10, 11, 12, 13)
        val endOfExistingItem = endOfNewItem.plusMinutes(1)

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem))
        // GIVEN
        val existingItem = TimeTrackingItem("existing item",
                startOfExistingItem, endOfExistingItem)
        sut.persist(existingItem)

        val newItem = TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem)

        // WHEN
        sut.persist(newItem)

        // THEN
        Assert.assertThat(
                stringWriter.toString(),
                `is`("2011-10-10_11:12:13 2020-10-10_11:12:13 new item"
                        + LINE_SEPARATOR
                        + "2020-10-10_11:12:13 2020-10-10_11:13:13 existing item"
                        + LINE_SEPARATOR))
    }

    @Theory
    fun shouldSplitOverlappingTimeIntervalWithoutEndIfNewItemEndsBefore(
            startOfNewItem: LocalDateTime) {
        val startOfExistingItem = LocalDateTime.of(2011, 10, 10, 11, 12, 13)
        val endOfNewItem = LocalDateTime.of(2020, 10, 10, 11, 12, 13)

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem))
        // GIVEN
        val existingItem = TimeTrackingItem("existing item",
                startOfExistingItem)
        sut.persist(existingItem)

        val newItem = TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem)

        // WHEN
        sut.persist(newItem)

        // THEN
        Assert.assertThat(stringWriter.toString(),
                `is`("2011-10-10_11:12:13 2020-10-10_11:12:13 new item"
                        + LINE_SEPARATOR + "2020-10-10_11:12:13 existing item"
                        + LINE_SEPARATOR))
    }

    @Test
    fun shouldChangeEndOfIntervalBeforeRemoveOverlappingIntervalAndChangeStartOfIntervalAfter() {
        // GIVEN
        val itemBeforeBefore = TimeTrackingItem(
                "Item before before", LocalDateTime.of(2010, 10, 10, 11, 12, 13),
                LocalDateTime.of(2010, 10, 10, 11, 14, 13))
        sut.persist(itemBeforeBefore)
        val itemBefore = TimeTrackingItem("Item before",
                LocalDateTime.of(2020, 10, 10, 11, 12, 13), LocalDateTime.of(2020, 10,
                10, 11, 14, 13))
        sut.persist(itemBefore)
        val overlappedItem = TimeTrackingItem(
                "Overlapped item", LocalDateTime.of(2020, 10, 10, 11, 14, 13),
                LocalDateTime.of(2020, 10, 10, 11, 15, 13))
        sut.persist(overlappedItem)
        val itemAfter = TimeTrackingItem("Item after",
                LocalDateTime.of(2020, 10, 10, 11, 15, 13), LocalDateTime.of(2020, 10,
                10, 11, 17, 13))
        sut.persist(itemAfter)
        val itemAfterAfter = TimeTrackingItem(
                "Item even after", LocalDateTime.of(2020, 10, 10, 11, 17, 13),
                LocalDateTime.of(2020, 10, 10, 11, 19, 13))
        sut.persist(itemAfterAfter)

        val newItem = TimeTrackingItem("new item",
                LocalDateTime.of(2020, 10, 10, 11, 13, 13), LocalDateTime.of(2020, 10,
                10, 11, 16, 13))

        // WHEN
        sut.persist(newItem)

        // THEN

        Assert.assertThat(
                stringWriter.toString(),
                `is`("2010-10-10_11:12:13 2010-10-10_11:14:13 Item before before"
                        + LINE_SEPARATOR
                        + "2020-10-10_11:12:13 2020-10-10_11:13:13 Item before"
                        + LINE_SEPARATOR
                        + "2020-10-10_11:13:13 2020-10-10_11:16:13 new item"
                        + LINE_SEPARATOR
                        + "2020-10-10_11:16:13 2020-10-10_11:17:13 Item after"
                        + LINE_SEPARATOR
                        + "2020-10-10_11:17:13 2020-10-10_11:19:13 Item even after"
                        + LINE_SEPARATOR))
    }

    @Test
    fun shouldSplitCoveringExistingItem() {
        // GIVEN
        val coveringItem = TimeTrackingItem("covering",
                LocalDateTime.of(2012, 1, 1, 10, 0, 0), LocalDateTime.of(2012, 1, 1,
                13, 0, 0))
        sut.persist(coveringItem)
        val coveredItem = TimeTrackingItem("newItem",
                LocalDateTime.of(2012, 1, 1, 11, 0, 0), LocalDateTime.of(2012, 1, 1,
                12, 0, 0))
        sut.persist(coveredItem)
        // WHEN

        // THEN
        assertThatFileMatches(
                "2012-01-01_10:00:00 2012-01-01_11:00:00 covering",
                "2012-01-01_11:00:00 2012-01-01_12:00:00 newItem",
                "2012-01-01_12:00:00 2012-01-01_13:00:00 covering")
    }

    @Test
    fun shouldNotChangeOldNonOverlappingItem() {
        // GIVEN
        val oldItem = TimeTrackingItem("old item",
                LocalDateTime.of(2010, 10, 10, 11, 12, 13), LocalDateTime.of(2010, 10,
                10, 11, 14, 13))
        sut.persist(oldItem)

        val newItem = TimeTrackingItem("old item",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13))

        // WHEN
        sut.persist(newItem)

        // THEN
        assertThatFileMatches(
                "2010-10-10_11:12:13 2010-10-10_11:14:13 old item",
                "2011-10-10_11:12:13 old item")
    }

    private fun assertThatFileMatches(vararg lines: String) {
        val expectedText = StringBuilder()
        for (line in lines) {
            expectedText.append(line).append(LINE_SEPARATOR)
        }
        assertThat(stringWriter.toString(), `is`(expectedText.toString()))
    }

    @Test
    fun shouldChangeUpdateItems() {
        // GIVEN
        val oldItem = TimeTrackingItem("old item",
                LocalDateTime.of(2010, 10, 10, 11, 12, 13), LocalDateTime.of(2010, 10,
                10, 11, 14, 13))
        sut.persist(oldItem)

        // WHEN
        sut.updateActivitities(setOf(oldItem), "new item")

        // THEN
        assertThatFileMatches("2010-10-10_11:12:13 2010-10-10_11:14:13 new item")
    }

    @Test
    fun shouldOnlyChangeGivenItems() {
        // GIVEN
        val oldItem = TimeTrackingItem("old item",
                LocalDateTime.of(2010, 10, 10, 11, 12, 13), LocalDateTime.of(2010, 10,
                10, 11, 14, 13))
        sut.persist(oldItem)

        // WHEN
        sut.updateActivitities(emptyList(), "new item")

        // THEN
        assertThatFileMatches("2010-10-10_11:12:13 2010-10-10_11:14:13 old item")
    }

    @Test
    fun shouldNotWriteLaterItemBeforeNewItem() {
        // GIVEN
        val oldItem = TimeTrackingItem("old item",
                LocalDateTime.of(2010, 10, 10, 11, 12, 13),
                LocalDateTime.of(2010, 10, 10, 11, 14, 13))
        sut.persist(oldItem)

        val newItem = TimeTrackingItem("new item",
                LocalDateTime.of(2010, 10, 10, 10, 12, 13),
                LocalDateTime.of(2010, 10, 10, 10, 14, 13))

        // WHEN
        sut.persist(newItem)

        // THEN
        assertThatFileMatches(
                "2010-10-10_10:12:13 2010-10-10_10:14:13 new item",
                "2010-10-10_11:12:13 2010-10-10_11:14:13 old item")
    }


    companion object {
        @JvmField
        @field:DataPoints
        val SAMPLE_DATE_TIMES = arrayOf(LocalDateTime.of(2011, 10, 10, 11, 12, 13), LocalDateTime.of(2010, 10, 10, 11, 12, 13), LocalDateTime.of(2012, 10, 10, 11, 12, 13))
        private val LINE_SEPARATOR = System.getProperty("line.separator")

    }
}
