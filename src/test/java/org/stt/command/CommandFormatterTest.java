package org.stt.command;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
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
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.stt.STTItemPersister;
import org.stt.persistence.stt.STTItemReader;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.time.DateTimes;

import javax.inject.Provider;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class CommandFormatterTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private CommandFormatter sut;
    private TimeTrackingItemQueries timeTrackingItemQueries;

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

    private STTItemPersister itemWriter;
    private Activities activities;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        File tempFile = tempFolder.newFile();
        Supplier<Reader> readerSupplier = () -> {
            try {
                return new InputStreamReader(new FileInputStream(tempFile), "UTF8");
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
        Provider<ItemReader> itemReaderProvider = () -> new STTItemReader(readerSupplier.get());
        itemWriter = new STTItemPersister(readerSupplier::get,
                () -> {
                    try {
                        return new FileWriter(tempFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        timeTrackingItemQueries = new TimeTrackingItemQueries(itemReaderProvider, Optional.empty());
        activities = new Activities(itemWriter, timeTrackingItemQueries, Optional.empty());
        sut = new CommandFormatter();
    }


    @Test
    public void itemToCommandShouldUseSinceIfEndIsMissing() {
        // GIVEN
        TimeTrackingItem item = new TimeTrackingItem("test", LocalDateTime.of(2000,
                1, 1, 1, 1, 1));

        // WHEN
        String result = sut.asNewItemCommandText(item);

        // THEN
        assertThat(result, is("test since 2000.01.01 01:01:01"));
    }

    @Test
    public void itemToCommandShouldUseFromToIfEndIsNotMissing() {
        // GIVEN
        TimeTrackingItem item = new TimeTrackingItem("test", LocalDateTime.of(2000,
                1, 1, 1, 1, 1), LocalDateTime.of(2000, 1, 1, 1, 1, 1));

        // WHEN
        String result = sut.asNewItemCommandText(item);

        // THEN
        assertThat(result,
                is("test from 2000.01.01 01:01:01 to 2000.01.01 01:01:01"));
    }

    @Test
    public void itemToCommandShouldUseLongFormatIfEndIsTomorrow() {
        // GIVEN
        LocalDateTime expectedStart = LocalDateTime.now();
        LocalDateTime expectedEnd = LocalDateTime.now().plusDays(1);
        TimeTrackingItem item = new TimeTrackingItem("test", expectedStart,
                expectedEnd);

        // WHEN
        String result = sut.asNewItemCommandText(item);

        // THEN
        String startString = DateTimes.DATE_TIME_FORMATTER_HH_MM_SS
                .format(expectedStart);
        String endString = DateTimes.DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS
                .format(expectedEnd);
        assertThat(result, is("test from " + startString + " to " + endString));
    }

    @Test
    public void shouldParseSince7_00() {
        // GIVEN

        // WHEN
        executeCommand("test since 7:00");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        LocalDateTime start = item.getStart();
        assertThatTimeIsTodayWith(start, 7, 0, 0);
    }

    @Test
    public void shouldParseAt7_00() {
        // GIVEN

        // WHEN
        executeCommand("test at 7:00");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        LocalDateTime start = item.getStart();
        assertThatTimeIsTodayWith(start, 7, 0, 0);
    }

    @Test
    public void shouldParseSince03_12_11() {
        // GIVEN

        // WHEN
        executeCommand("test since 03:12:11");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        LocalDateTime start = item.getStart();
        assertThatTimeIsTodayWith(start, 3, 12, 11);
    }

    @Test
    public void shouldParseSince13_37() {
        // GIVEN

        // WHEN
        executeCommand("test since 13:37");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        LocalDateTime start = item.getStart();
        assertThatTimeIsTodayWith(start, 13, 37, 0);
    }

    @Test
    public void shouldParseSince2000_01_01_13_37() {
        // GIVEN

        // WHEN
        executeCommand("test since 2000.01.01 13:37:00");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        LocalDateTime start = item.getStart();
        assertThat(start, is(LocalDateTime.of(2000, 1, 1, 13, 37, 0)));
    }

    private void assertThatTimeIsTodayWith(LocalDateTime time, int hourOfDay,
                                           int minuteOfHour, int secondOfMinute) {
        assertThat(time.getHour(), is(hourOfDay));
        assertThat(time.getMinute(), is(minuteOfHour));
        assertThat(time.getSecond(), is(secondOfMinute));
        assertThat(time.get(ChronoField.MILLI_OF_SECOND), is(0));
        assertThat(time.toLocalDate(), is(LocalDate.now()));
    }

    @Test
    public void shouldParseFromXtoYCommand() {

        // GIVEN
        LocalDateTime expectedStart = LocalDate.now().atStartOfDay()
                .withHour(12);
        LocalDateTime expectedEnd = LocalDate.now().atStartOfDay()
                .withHour(13);
        TimeTrackingItem expectedItem = new TimeTrackingItem("comment",
                expectedStart, expectedEnd);
        // WHEN
        Optional<TimeTrackingItem> result = executeCommand("comment from 12:00 to 13:00");

        // THEN
        assertThat(result, is(Optional.of(expectedItem)));
    }

    @Test
    public void shouldParseSinceXUntilYCommand() {
        // GIVEN
        LocalDateTime expectedStart = LocalDate.now().atStartOfDay()
                .withHour(12);
        LocalDateTime expectedEnd = LocalDate.now().atStartOfDay()
                .withHour(13);
        TimeTrackingItem expectedItem = new TimeTrackingItem("comment",
                expectedStart, expectedEnd);
        // WHEN
        Optional<TimeTrackingItem> result = executeCommand("comment since 12:00 until 13:00");

        // THEN
        assertThat(result, is(Optional.of(expectedItem)));
    }

    @Test
    public void shouldParseFromToWithSpaceInComment() {
        // GIVEN

        // WHEN
        Optional<TimeTrackingItem> result = executeCommand("no t from 2014.06.22 14:43:14 to 2014.06.22 14:58:41");

        // THEN
        TimeTrackingItem item = result.get();
        assertThat(item.getStart(), is(LocalDateTime.of(2014, 6, 22, 14, 43, 14)));
        assertThat(item.getEnd().get(),
                is(LocalDateTime.of(2014, 6, 22, 14, 58, 41)));
    }

    @Test
    public void shouldParseFromXtoYWithoutFromCommand() {

        // GIVEN
        LocalDateTime expectedStart = LocalDate.now().atStartOfDay()
                .withHour(12);
        LocalDateTime expectedEnd = LocalDate.now().atStartOfDay()
                .withHour(13);
        TimeTrackingItem expectedItem = new TimeTrackingItem("com ment",
                expectedStart, expectedEnd);
        // WHEN
        Optional<TimeTrackingItem> result = executeCommand("com ment 12:00 to 13:00");

        // THEN
        assertThat(result, is(Optional.of(expectedItem)));
    }

    @Test
    public void shouldDoNothingOnResumeLastAndActiveItem() {
        // GIVEN
        TimeTrackingItem unfinishedItem = createUnfinishedItem();
        givenCurrentTimeTrackingItem(unfinishedItem);

        // WHEN
        executeCommand("resume last");

        // THEN
        timeTrackingItemQueries.sourceChanged(null);
        TimeTrackingItem timeTrackingItem = timeTrackingItemQueries.getOngoingItem().get();
        assertThat(timeTrackingItem, is(unfinishedItem));
    }

    @Test
    public void shouldStartNewItemNowOnResumeLastAndPreviouslyFinishedItem() {
        // GIVEN
        TimeTrackingItem finishedItem = new TimeTrackingItem("last item",
                LocalDateTime.of(2014, 6, 22, 14, 43, 14),
                LocalDateTime.of(2015, 6, 22, 14, 43, 14));
        givenCurrentTimeTrackingItem(finishedItem);

        // WHEN
        executeCommand("resume last");

        // THEN
        timeTrackingItemQueries.sourceChanged(null);
        TimeTrackingItem timeTrackingItem = timeTrackingItemQueries.getOngoingItem().get();
        assertThat(timeTrackingItem.getActivity(), is("last item"));
        assertThat(!timeTrackingItem.getStart().isAfter(LocalDateTime.now()), is(true));
        assertThat(timeTrackingItem.getEnd(), is(Optional.empty()));
    }


    @Test
    public void shouldEndCurrentItemOnFINCommand() throws IOException {
        // GIVEN
        TimeTrackingItem unfinished = createUnfinishedItem();
        givenCurrentTimeTrackingItem(unfinished);

        // WHEN
        executeCommand("fin");

        // THEN
        TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
        assertThat(item.getEnd(), not(Optional.empty()));
    }

    private TimeTrackingItem createUnfinishedItem() {
        return new TimeTrackingItem("", LocalDateTime.now().minus(1, ChronoUnit.MILLIS));
    }

    @Test
    public void shouldWriteCommandsAsNewItems() throws IOException {
        // GIVEN

        // WHEN
        executeCommand("test");

        assertThatNewItemWasWritten("test");
    }

    private void assertThatNewItemWasWritten(String testComment)
            throws IOException {
        TimeTrackingItem newTimeTrackingItem = retrieveWrittenTimeTrackingItem();
        assertThat(newTimeTrackingItem.getActivity(), is(testComment));
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
                is(Matchers.lessThanOrEqualTo(LocalDateTime.now()
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
                is(Matchers.lessThanOrEqualTo(LocalDateTime.now()
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
                is(Matchers.lessThanOrEqualTo(LocalDateTime.now()
                        .minusHours(hoursAgo))));
    }

    @Test
    public void shouldAddNonOverlappingPrecedingItem() {
        // GIVEN
        executeCommand("aaa from 2014.06.22 10:00 to 2014.06.22 16:00");

        // WHEN
        executeCommand("bbb from 2014.06.21 10:00 to 2014.06.21 16:00");

        // THEN
        TimeTrackingItem[] timeTrackingItems = timeTrackingItemQueries.queryAllItems()
                .toArray(TimeTrackingItem[]::new);
        assertThat(timeTrackingItems[1].getStart().toLocalDate(), is(LocalDate.of(2014, 6, 22)));
    }

    private Optional<TimeTrackingItem> executeCommand(String command) {
        org.stt.command.Command cmdToExec = sut.parse(command);
        TestCommandHandler testCommandHandler = new TestCommandHandler();
        cmdToExec.accept(testCommandHandler);
        cmdToExec.accept(activities);
        return Optional.ofNullable(testCommandHandler.resultItem);
    }

    private static class TestCommandHandler implements CommandHandler {
        TimeTrackingItem resultItem;

        @Override
        public void addNewActivity(NewActivity command) {
            resultItem = command.newItem;
        }

        @Override
        public void endCurrentActivity(EndCurrentItem command) {
        }

        @Override
        public void removeActivity(RemoveActivity command) {

        }

        @Override
        public void removeActivityAndFillGap(RemoveActivity command) {

        }

        @Override
        public void resumeActivity(ResumeActivity command) {

        }

        @Override
        public void resumeLastActivity(ResumeLastActivity command) {
        }
    }

    private TimeTrackingItem retrieveItemWhenCommandIsExecuted(String command) {
        executeCommand(command);
        return retrieveWrittenTimeTrackingItem();
    }

    private TimeTrackingItem retrieveWrittenTimeTrackingItem() {
        timeTrackingItemQueries.sourceChanged(null);
        Collection<TimeTrackingItem> allItems = timeTrackingItemQueries.queryAllItems().collect(Collectors.toList());
        assertThat(allItems, IsCollectionWithSize.hasSize(1));
        return allItems.iterator().next();
    }

    private void givenCurrentTimeTrackingItem(TimeTrackingItem item) {
        itemWriter.persist(item);
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