package org.stt.command;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.query.TimeTrackingItemQueries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CommandParserTest {
    private CommandParser sut;
    @Mock
    private TimeTrackingItemQueries timeTrackingItemQueries;
    @Mock
    private ItemPersister itemPersister;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sut = new CommandParser(itemPersister, timeTrackingItemQueries);
    }

    @Test
    public void itemToCommandShouldUseSinceIfEndIsMissing() {
        // GIVEN
        TimeTrackingItem item = new TimeTrackingItem("test", new DateTime(2000,
                1, 1, 1, 1, 1));

        // WHEN
        String result = sut.itemToCommand(item);

        // THEN
        assertThat(result, is("test since 2000.01.01 01:01:01"));
    }

    @Test
    public void itemToCommandShouldUseFromToIfEndIsNotMissing() {
        // GIVEN
        TimeTrackingItem item = new TimeTrackingItem("test", new DateTime(2000,
                1, 1, 1, 1, 1), new DateTime(2000, 1, 1, 1, 1, 1));

        // WHEN
        String result = sut.itemToCommand(item);

        // THEN
        assertThat(result,
                is("test from 2000.01.01 01:01:01 to 2000.01.01 01:01:01"));
    }

    @Test
    public void itemToCommandShouldUseLongFormatIfEndIsTomorrow() {
        // GIVEN
        DateTime expectedStart = DateTime.now();
        DateTime expectedEnd = DateTime.now().plusDays(1);
        TimeTrackingItem item = new TimeTrackingItem("test", expectedStart,
                expectedEnd);

        // WHEN
        String result = sut.itemToCommand(item);

        // THEN
        String startString = CommandParser.FORMAT_HOUR_MINUTES_SECONDS
                .print(expectedStart);
        String endString = CommandParser.FORMAT_YEAR_MONTH_HOUR_MINUTES_SECONDS
                .print(expectedEnd);
        assertThat(result, is("test from " + startString + " to " + endString));
    }

}