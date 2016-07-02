package org.stt;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.joda.time.DateTime;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.stt.command.ToItemWriterCommandHandler;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.stt.STTItemPersister;
import org.stt.persistence.stt.STTItemReader;
import org.stt.query.DefaultTimeTrackingItemQueries;
import org.stt.query.TimeTrackingItemQueries;

import java.io.*;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class ToItemWriterCommandHandlerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    ItemPersister itemWriter;
    TimeTrackingItemQueries timeTrackingItemQueries;

    private ToItemWriterCommandHandler sut;

    @DataPoints
    public static Command[] minuteFormats = {min("test %smins ago"),
            min("test %s mins ago"), min("test %smin ago"),
            min("test\n%s minutes ago"), min("test one\ntest two %smin ago ")};

    @DataPoints
    public static Command[] secondFormats = {secs("test %ss ago"),
            secs("test %s sec ago"), secs("test %ssecs ago"),
            secs("test\n%s second ago"), secs("test %sseconds ago"),
            secs("from here to there %sseconds ago")};

    @DataPoints
    public static Command[] hourFormats = {hours("test %sh ago"),
            hours("test %shr ago"), hours("test %s hrs ago"),
            hours("test\n%shour ago"), hours("test %s hours ago"),
            hours("left 3 hours ago %s hours ago")};

    @Before
    public void setup() throws IOException {
        File tempFile = tempFolder.newFile();
        Supplier<Reader> readerSupplier = () -> {
            try {
                return new InputStreamReader(new FileInputStream(tempFile), "UTF8");
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
        ItemReaderProvider itemReaderProvider = () -> new STTItemReader(readerSupplier.get());
        itemWriter = new STTItemPersister(readerSupplier::get,
                () -> {
                    try {
                        return new FileWriter(tempFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        timeTrackingItemQueries = new DefaultTimeTrackingItemQueries(itemReaderProvider);
        sut = new ToItemWriterCommandHandler(itemWriter, timeTrackingItemQueries);
    }

    @Test
    public void resumeShouldCreateNewItemWithOldComment() {
        // GIVEN
        DateTime now = DateTime.now();
        TimeTrackingItem item = new TimeTrackingItem("for test",
                now.minusMinutes(20), now.minusMinutes(10));

        // WHEN
        sut.resumeGivenItem(item);

        // THEN
        TimeTrackingItem trackingItem = retrieveWrittenTimeTrackingItem();
        assertThat(trackingItem.getComment().get(), is("for test"));
        assertThat(trackingItem.getStart(),
                is(greaterThan(now.minusMinutes(5))));
    }

    @Test
    public void finShouldEndCurrentItem() throws IOException {
        // GIVEN
        TimeTrackingItem unfinished = createUnfinishedItem();
        givenCurrentTimeTrackingItem(unfinished);

        // WHEN
        sut.endCurrentItem();

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        assertThat(item.getEnd(), not(Optional.absent()));
    }

    @Test
    public void finWithEndTimeShouldEndCurrentItem() {

        // GIVEN
        TimeTrackingItem unfinished = createUnfinishedItem();
        givenCurrentTimeTrackingItem(unfinished);
        DateTime expectedEndTime = DateTime.now().plus(30000);

        // WHEN
        Optional<TimeTrackingItem> endCurrentItem = sut
                .endCurrentItem(expectedEndTime);

        // THEN
        assertThat(endCurrentItem.get().getEnd().get(), is(expectedEndTime));
    }

    @Test
    public void shouldParseSince7_00() {
        // GIVEN

        // WHEN
        sut.executeCommand("test since 7:00");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        DateTime start = item.getStart();
        assertThatTimeIsTodayWith(start, 7, 0, 0);
    }

    @Test
    public void shouldParseAt7_00() {
        // GIVEN

        // WHEN
        sut.executeCommand("test at 7:00");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        DateTime start = item.getStart();
        assertThatTimeIsTodayWith(start, 7, 0, 0);
    }

    @Test
    public void shouldParseSince03_12_11() {
        // GIVEN

        // WHEN
        sut.executeCommand("test since 03:12:11");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        DateTime start = item.getStart();
        assertThatTimeIsTodayWith(start, 3, 12, 11);
    }

    @Test
    public void shouldParseSince13_37() {
        // GIVEN

        // WHEN
        sut.executeCommand("test since 13:37");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        DateTime start = item.getStart();
        assertThatTimeIsTodayWith(start, 13, 37, 0);
    }

    @Test
    public void shouldParseSince2000_01_01_13_37() {
        // GIVEN

        // WHEN
        sut.executeCommand("test since 2000.01.01 13:37:00");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        DateTime start = item.getStart();
        assertThat(start, is(new DateTime(2000, 1, 1, 13, 37, 0)));
    }

    private void assertThatTimeIsTodayWith(DateTime time, int hourOfDay,
                                           int minuteOfHour, int secondOfMinute) {
        assertThat(time.getHourOfDay(), is(hourOfDay));
        assertThat(time.getMinuteOfHour(), is(minuteOfHour));
        assertThat(time.getSecondOfMinute(), is(secondOfMinute));
        assertThat(time.getMillisOfSecond(), is(0));
        assertThat(time.withTimeAtStartOfDay(), is(DateTime.now()
                .withTimeAtStartOfDay()));
    }

    @Test
    public void shouldParseFromXtoYCommand() {

        // GIVEN
        DateTime expectedStart = DateTime.now().withTimeAtStartOfDay()
                .withHourOfDay(12);
        DateTime expectedEnd = DateTime.now().withTimeAtStartOfDay()
                .withHourOfDay(13);
        TimeTrackingItem expectedItem = new TimeTrackingItem("comment",
                expectedStart, expectedEnd);
        // WHEN
        Optional<TimeTrackingItem> result = sut
                .executeCommand("comment from 12:00 to 13:00");

        // THEN
        assertThat(result, is(Optional.of(expectedItem)));
    }

    @Test
    public void shouldParseSinceXUntilYCommand() {
        // GIVEN
        DateTime expectedStart = DateTime.now().withTimeAtStartOfDay()
                .withHourOfDay(12);
        DateTime expectedEnd = DateTime.now().withTimeAtStartOfDay()
                .withHourOfDay(13);
        TimeTrackingItem expectedItem = new TimeTrackingItem("comment",
                expectedStart, expectedEnd);
        // WHEN
        Optional<TimeTrackingItem> result = sut
                .executeCommand("comment since 12:00 until 13:00");

        // THEN
        assertThat(result, is(Optional.of(expectedItem)));
    }

    @Test
    public void shouldParseFromToWithSpaceInComment() {
        // GIVEN

        // WHEN
        Optional<TimeTrackingItem> result = sut
                .executeCommand("no t from 2014.06.22 14:43:14 to 2014.06.22 14:58:41");

        // THEN
        TimeTrackingItem item = result.get();
        assertThat(item.getStart(), is(new DateTime(2014, 6, 22, 14, 43, 14)));
        assertThat(item.getEnd().get(),
                is(new DateTime(2014, 6, 22, 14, 58, 41)));
    }

    @Test
    public void shouldParseFromXtoYWithoutFromCommand() {

        // GIVEN
        DateTime expectedStart = DateTime.now().withTimeAtStartOfDay()
                .withHourOfDay(12);
        DateTime expectedEnd = DateTime.now().withTimeAtStartOfDay()
                .withHourOfDay(13);
        TimeTrackingItem expectedItem = new TimeTrackingItem("com ment",
                expectedStart, expectedEnd);
        // WHEN
        Optional<TimeTrackingItem> result = sut
                .executeCommand("com ment 12:00 to 13:00");

        // THEN
        assertThat(result, is(Optional.of(expectedItem)));
    }

    @Test
    public void shouldEndCurrentItemOnFINCommand() throws IOException {
        // GIVEN
        TimeTrackingItem unfinished = createUnfinishedItem();
        givenCurrentTimeTrackingItem(unfinished);

        // WHEN
        sut.executeCommand("fin");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        assertThat(item.getEnd(), not(Optional.absent()));
    }

    private TimeTrackingItem createUnfinishedItem() {
        return new TimeTrackingItem(null, DateTime.now().minusMillis(1));
    }

    @Test
    public void shouldWriteCommandsAsNewItems() throws IOException {
        // GIVEN

        // WHEN
        sut.executeCommand("test");

        assertThatNewItemWasWritten("test");
    }

    private void assertThatNewItemWasWritten(String testComment)
            throws IOException {
        TimeTrackingItem newTimeTrackingItem = retrieveWrittenTimeTrackingItem();
        assertThat(newTimeTrackingItem.getComment(),
                is(Optional.of(testComment)));
    }

    @Theory
    public void shouldParseMinutesAgoFormats(
            @TestedOn(ints = {0, 1, 10, 61}) int minutesAgo, Command format) {
        Assume.assumeTrue(format.isCategory("mins"));
        // GIVEN
        String command = format.supplyCommandFor(minutesAgo);

        // WHEN
        TimeTrackingItem item = retrieveItemWhenCommandIsExecuted(command);

        // THEN
        assertThat("Parameters: '" + minutesAgo + "' '" + command + "'",
                item.getStart(),
                is(Matchers.lessThanOrEqualTo(DateTime.now()
                        .minusMinutes(minutesAgo))));
    }

    @Theory
    public void shouldParseSecondsAgoFormats(
            @TestedOn(ints = {0, 1, 10, 61}) int secondsAgo, Command format) {
        Assume.assumeTrue(format.isCategory("secs"));
        // GIVEN
        String command = format.supplyCommandFor(secondsAgo);

        // WHEN
        TimeTrackingItem item = retrieveItemWhenCommandIsExecuted(command);

        // THEN
        assertThat("Parameters: '" + secondsAgo + "' '" + command + "'",
                item.getStart(),
                is(Matchers.lessThanOrEqualTo(DateTime.now()
                        .minusSeconds(secondsAgo))));
    }

    @Theory
    public void shouldParseHourAgoFormats(
            @TestedOn(ints = {0, 1, 10, 61}) int hoursAgo, Command format) {
        Assume.assumeTrue(format.isCategory("hours"));
        // GIVEN
        String command = format.supplyCommandFor(hoursAgo);

        // WHEN
        TimeTrackingItem item = retrieveItemWhenCommandIsExecuted(command);

        // THEN
        assertThat("Parameters: '" + hoursAgo + "' '" + command + "'",
                item.getStart(),
                is(Matchers.lessThanOrEqualTo(DateTime.now()
                        .minusHours(hoursAgo))));
    }


    @Test
    public void shouldDelegateDelete() throws IOException {
        // GIVEN
        TimeTrackingItem item = new TimeTrackingItem(null, DateTime.now());

        // WHEN
        sut.delete(item);

        // THEN
        assertEmptyItemList();
    }

    private void assertEmptyItemList() {
        assertThat(timeTrackingItemQueries.queryAllItems(), is(Collections.emptyList()));
    }

    @Test
    public void shouldNotNonOverlappingFollowUpItem() {
        // GIVEN
        sut.executeCommand("aaa from 2014.06.22 10:00 to 2014.06.22 16:00");

        // WHEN
        sut.executeCommand("bbb from 2014.06.21 10:00 to 2014.06.21 16:00");

        // THEN
        TimeTrackingItem[] timeTrackingItems = timeTrackingItemQueries.queryAllItems().toArray(new TimeTrackingItem[2]);
        for (TimeTrackingItem item : timeTrackingItems) {
            System.out.println(item);
        }
        assertThat(timeTrackingItems[1].getStart().withTimeAtStartOfDay(), is(new DateTime(2014, 6, 22, 0, 0)));
    }

    private TimeTrackingItem retrieveItemWhenCommandIsExecuted(String command) {
        sut.executeCommand(command);
        return retrieveWrittenTimeTrackingItem();
    }

    private TimeTrackingItem retrieveWrittenTimeTrackingItem() {
        Collection<TimeTrackingItem> allItems = timeTrackingItemQueries.queryAllItems();
        assertThat(allItems, IsCollectionWithSize.hasSize(1));
        return allItems.iterator().next();
    }

    private void givenCurrentTimeTrackingItem(TimeTrackingItem item) {
        try {
            itemWriter.insert(item);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Command min(String command) {
        return new Command(command, "mins");
    }

    public static Command secs(String command) {
        return new Command(command, "secs");
    }

    public static Command hours(String command) {
        return new Command(command, "hours");
    }

    private static class Command {
        private final String commandString;
        private final String category;

        public Command(String commandString, String category) {
            assert commandString != null;
            assert category != null;
            this.commandString = commandString;
            this.category = category;
        }

        public boolean isCategory(String category) {
            return this.category.equals(category);
        }

        public String supplyCommandFor(int amount) {
            return commandString.replace("%s", Integer.toString(amount));
        }
    }
}
