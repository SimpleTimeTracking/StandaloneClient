package org.stt.importer;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.stt.STTItemPersister;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class STTItemWriterTest {

    private static final String LINE_SEPERATOR = System
            .getProperty("line.separator");
    @DataPoints
    public static LocalDateTime[] sampleDateTimes = new LocalDateTime[]{
            LocalDateTime.of(2011, 10, 10, 11, 12, 13),
            LocalDateTime.of(2010, 10, 10, 11, 12, 13),
            LocalDateTime.of(2012, 10, 10, 11, 12, 13)};
    private StringWriter stringWriter;
    private STTItemPersister sut;

    @Before
    public void setUp() {
        stringWriter = new StringWriter();

        sut = new STTItemPersister(() -> new StringReader(stringWriter.toString()), () -> stringWriter = new StringWriter());
    }

    @Test(expected = NullPointerException.class)
    public void writeNullObjectFails() throws IOException {

        // WHEN
        sut.persist(null);

        // THEN
        // Exception expected
    }

    @Test
    public void writeCommentSucceeds() throws IOException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("the comment",
                LocalDateTime.now());

        // WHEN
        sut.persist(theItem);

        // THEN
        Assert.assertThat(stringWriter.toString(),
                containsString("the comment"));
    }

    @Test
    public void writeStartSucceeds() throws IOException {

        // GIVEN
        LocalDateTime theTime = LocalDateTime.of(2011, 10, 12, 13, 14, 15);
        TimeTrackingItem theItem = new TimeTrackingItem("", theTime);

        // WHEN
        sut.persist(theItem);

        // THEN
        Assert.assertThat(stringWriter.toString(),
                containsString("2011-10-12_13:14:15"));
    }

    @Test
    public void writeEndSucceeds() throws IOException {

        // GIVEN
        LocalDateTime start = LocalDateTime.of(2011, 10, 12, 13, 14, 15);
        LocalDateTime end = LocalDateTime.of(2012, 10, 12, 13, 14, 15);

        TimeTrackingItem theItem = new TimeTrackingItem("", start, end);

        // WHEN
        sut.persist(theItem);

        // THEN
        Assert.assertThat(stringWriter.toString(),
                containsString("2012-10-12_13:14:15"));
    }

    @Test
    public void writeCompleteEntrySucceeds() throws IOException {

        // GIVEN
        LocalDateTime start = LocalDateTime.of(2011, 10, 12, 13, 14, 15);

        LocalDateTime end = LocalDateTime.of(2012, 10, 12, 13, 14, 15);
        TimeTrackingItem theItem = new TimeTrackingItem("the comment", start,
                end);

        // WHEN
        sut.persist(theItem);

        // THEN
        Assert.assertThat(
                stringWriter.toString(),
                containsString("2011-10-12_13:14:15 2012-10-12_13:14:15 the comment"));
    }

    @Test
    public void writeMultiLineEntrySucceeds() throws IOException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem(
                "this is\n a multiline\n string\n with different separators",
                LocalDateTime.now());

        // WHEN
        sut.persist(theItem);

        // THEN
        Assert.assertThat(
                stringWriter.toString(),
                endsWith("this is\\n a multiline\\n string\\n with different separators"
                        + LINE_SEPERATOR));

    }

    @Test
    public void itemsCanBeDeleted() throws IOException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("testitem",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13));
        TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
                LocalDateTime.of(2014, 10, 10, 11, 12, 13));
        sut.persist(theItem);
        sut.persist(theItem2);

        // when
        sut.delete(theItem2);

        // then
        Assert.assertThat(stringWriter.toString(),
                is("2011-10-10_11:12:13 2014-10-10_11:12:13 testitem"
                        + LINE_SEPERATOR));
    }

    @Test
    public void itemCanBeReplaced() throws IOException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("testitem",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13));
        TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
                LocalDateTime.now());
        sut.persist(theItem2);

        // when
        sut.replace(theItem2, theItem);

        // then
        Assert.assertThat(stringWriter.toString(),
                is("2011-10-10_11:12:13 testitem" + LINE_SEPERATOR));
    }

    @Test
    public void shouldWriteItemsWithMultipleWhitespaces() throws IOException {
        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("item with 2  spaces",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13));

        // when
        sut.persist(theItem);

        // then
        Assert.assertThat(stringWriter.toString(),
                is("2011-10-10_11:12:13 item with 2  spaces" + LINE_SEPERATOR));
    }

    @Theory
    public void shouldRemoveCoveredTimeIntervalsIfNewItemHasNoEnd(
            LocalDateTime startOfNewItem) throws IOException {
        LocalDateTime startOfExistingItem = LocalDateTime.of(2011, 10, 10, 11, 12, 13);

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("testitem",
                startOfExistingItem);
        sut.persist(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("testitem2",
                startOfExistingItem);

        // WHEN
        sut.persist(newItem);

        // THEN
        Assert.assertThat(stringWriter.toString(),
                is("2011-10-10_11:12:13 testitem2" + LINE_SEPERATOR));
    }

    @Test
    public void shouldSetEndTimeIfNewItemIsStarted() throws IOException {
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("testitem",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13));
        sut.persist(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("testitem2",
                LocalDateTime.of(2011, 10, 10, 11, 12, 14));

        // WHEN
        sut.persist(newItem);

        // THEN
        Assert.assertThat(stringWriter.toString(),
                is("2011-10-10_11:12:13 2011-10-10_11:12:14 testitem"
                        + LINE_SEPERATOR + "2011-10-10_11:12:14 testitem2"
                        + LINE_SEPERATOR));

    }

    @Theory
    public void shouldRemoveCoveredTimeIntervalsIfCoveredByNewItem(
            LocalDateTime startOfNewItem) throws IOException {
        LocalDateTime startOfExistingItem = LocalDateTime.of(2011, 10, 10, 11, 12, 13);
        LocalDateTime endOfNewItem = LocalDateTime.of(2020, 10, 10, 11, 12, 13);

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("existing item",
                startOfExistingItem, endOfNewItem);
        sut.persist(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem);

        // WHEN
        sut.persist(newItem);

        // THEN
        Assert.assertThat(stringWriter.toString(),
                is("2011-10-10_11:12:13 2020-10-10_11:12:13 new item"
                        + LINE_SEPERATOR));
    }

    @Theory
    public void shouldSplitOverlappingTimeIntervalWithEndIfNewItemEndsBefore(
            LocalDateTime startOfNewItem) throws IOException {
        LocalDateTime startOfExistingItem = LocalDateTime.of(2011, 10, 10, 11, 12, 13);
        LocalDateTime endOfNewItem = LocalDateTime.of(2020, 10, 10, 11, 12, 13);
        LocalDateTime endOfExistingItem = endOfNewItem.plusMinutes(1);

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("existing item",
                startOfExistingItem, endOfExistingItem);
        sut.persist(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem);

        // WHEN
        sut.persist(newItem);

        // THEN
        Assert.assertThat(
                stringWriter.toString(),
                is("2011-10-10_11:12:13 2020-10-10_11:12:13 new item"
                        + LINE_SEPERATOR
                        + "2020-10-10_11:12:13 2020-10-10_11:13:13 existing item"
                        + LINE_SEPERATOR));
    }

    @Theory
    public void shouldSplitOverlappingTimeIntervalWithoutEndIfNewItemEndsBefore(
            LocalDateTime startOfNewItem) throws IOException {
        LocalDateTime startOfExistingItem = LocalDateTime.of(2011, 10, 10, 11, 12, 13);
        LocalDateTime endOfNewItem = LocalDateTime.of(2020, 10, 10, 11, 12, 13);

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("existing item",
                startOfExistingItem);
        sut.persist(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem);

        // WHEN
        sut.persist(newItem);

        // THEN
        Assert.assertThat(stringWriter.toString(),
                is("2011-10-10_11:12:13 2020-10-10_11:12:13 new item"
                        + LINE_SEPERATOR + "2020-10-10_11:12:13 existing item"
                        + LINE_SEPERATOR));
    }

    @Test
    public void shouldChangeEndOfIntervalBeforeRemoveOverlappingIntervalAndChangeStartOfIntervalAfter()
            throws IOException {
        // GIVEN
        TimeTrackingItem itemBeforeBefore = new TimeTrackingItem(
                "Item before before", LocalDateTime.of(2010, 10, 10, 11, 12, 13),
                LocalDateTime.of(2010, 10, 10, 11, 14, 13));
        sut.persist(itemBeforeBefore);
        TimeTrackingItem itemBefore = new TimeTrackingItem("Item before",
                LocalDateTime.of(2020, 10, 10, 11, 12, 13), LocalDateTime.of(2020, 10,
                10, 11, 14, 13));
        sut.persist(itemBefore);
        TimeTrackingItem overlappedItem = new TimeTrackingItem(
                "Overlapped item", LocalDateTime.of(2020, 10, 10, 11, 14, 13),
                LocalDateTime.of(2020, 10, 10, 11, 15, 13));
        sut.persist(overlappedItem);
        TimeTrackingItem itemAfter = new TimeTrackingItem("Item after",
                LocalDateTime.of(2020, 10, 10, 11, 15, 13), LocalDateTime.of(2020, 10,
                10, 11, 17, 13));
        sut.persist(itemAfter);
        TimeTrackingItem itemAfterAfter = new TimeTrackingItem(
                "Item even after", LocalDateTime.of(2020, 10, 10, 11, 17, 13),
                LocalDateTime.of(2020, 10, 10, 11, 19, 13));
        sut.persist(itemAfterAfter);

        TimeTrackingItem newItem = new TimeTrackingItem("new item",
                LocalDateTime.of(2020, 10, 10, 11, 13, 13), LocalDateTime.of(2020, 10,
                10, 11, 16, 13));

        // WHEN
        sut.persist(newItem);

        // THEN

        Assert.assertThat(
                stringWriter.toString(),
                is("2010-10-10_11:12:13 2010-10-10_11:14:13 Item before before"
                        + LINE_SEPERATOR
                        + "2020-10-10_11:12:13 2020-10-10_11:13:13 Item before"
                        + LINE_SEPERATOR
                        + "2020-10-10_11:13:13 2020-10-10_11:16:13 new item"
                        + LINE_SEPERATOR
                        + "2020-10-10_11:16:13 2020-10-10_11:17:13 Item after"
                        + LINE_SEPERATOR
                        + "2020-10-10_11:17:13 2020-10-10_11:19:13 Item even after"
                        + LINE_SEPERATOR));
    }

    @Test
    public void shouldSplitCoveringExistingItem() throws IOException {
        // GIVEN
        TimeTrackingItem coveringItem = new TimeTrackingItem("covering",
                LocalDateTime.of(2012, 1, 1, 10, 0, 0), LocalDateTime.of(2012, 1, 1,
                13, 0, 0));
        sut.persist(coveringItem);
        TimeTrackingItem coveredItem = new TimeTrackingItem("newItem",
                LocalDateTime.of(2012, 1, 1, 11, 0, 0), LocalDateTime.of(2012, 1, 1,
                12, 0, 0));
        sut.persist(coveredItem);
        // WHEN

        // THEN
        assertThatFileMatches(
                "2012-01-01_10:00:00 2012-01-01_11:00:00 covering",
                "2012-01-01_11:00:00 2012-01-01_12:00:00 newItem",
                "2012-01-01_12:00:00 2012-01-01_13:00:00 covering");
    }

    @Test
    public void shouldNotChangeOldNonOverlappingItem() throws IOException {
        // GIVEN
        TimeTrackingItem oldItem = new TimeTrackingItem("old item",
                LocalDateTime.of(2010, 10, 10, 11, 12, 13), LocalDateTime.of(2010, 10,
                10, 11, 14, 13));
        sut.persist(oldItem);

        TimeTrackingItem newItem = new TimeTrackingItem("old item",
                LocalDateTime.of(2011, 10, 10, 11, 12, 13));

        // WHEN
        sut.persist(newItem);

        // THEN
        assertThatFileMatches(
                "2010-10-10_11:12:13 2010-10-10_11:14:13 old item",
                "2011-10-10_11:12:13 old item");
    }

    private void assertThatFileMatches(String... lines) {
        StringBuilder expectedText = new StringBuilder();
        for (String line : lines) {
            expectedText.append(line).append(LINE_SEPERATOR);
        }
        assertThat(stringWriter.toString(), is(expectedText.toString()));
    }

    @Test
    public void shouldChangeUpdateItems() {
        // GIVEN
        TimeTrackingItem oldItem = new TimeTrackingItem("old item",
                LocalDateTime.of(2010, 10, 10, 11, 12, 13), LocalDateTime.of(2010, 10,
                10, 11, 14, 13));
        sut.persist(oldItem);

        // WHEN
        sut.updateActivitities(Collections.singleton(oldItem), "new item");

        // THEN
        assertThatFileMatches("2010-10-10_11:12:13 2010-10-10_11:14:13 new item");
    }

    @Test
    public void shouldOnlyChangeGivenItems() {
        // GIVEN
        TimeTrackingItem oldItem = new TimeTrackingItem("old item",
                LocalDateTime.of(2010, 10, 10, 11, 12, 13), LocalDateTime.of(2010, 10,
                10, 11, 14, 13));
        sut.persist(oldItem);

        // WHEN
        sut.updateActivitities(Collections.emptyList(), "new item");

        // THEN
        assertThatFileMatches("2010-10-10_11:12:13 2010-10-10_11:14:13 old item");
    }

}
