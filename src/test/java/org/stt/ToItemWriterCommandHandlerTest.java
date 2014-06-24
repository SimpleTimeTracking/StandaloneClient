package org.stt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;
import org.stt.searching.ItemSearcher;

import com.google.common.base.Optional;

@RunWith(Theories.class)
public class ToItemWriterCommandHandlerTest {
	@Mock
	ItemWriter itemWriter;

	@Mock
	ItemSearcher itemSearcher;

	private ToItemWriterCommandHandler sut;

	@DataPoints
	public static Command[] minuteFormats = { min("test %smins ago"),
			min("test %s mins ago"), min("test %smin ago"),
			min("test\n%s minutes ago") };

	@DataPoints
	public static Command[] secondFormats = { secs("test %ss ago"),
			secs("test %s sec ago"), secs("test %ssecs ago"),
			secs("test\n%s second ago"), secs("test %sseconds ago") };

	@DataPoints
	public static Command[] hourFormats = { hours("test %sh ago"),
			hours("test %shr ago"), hours("test %s hrs ago"),
			hours("test\n%shour ago"), hours("test %s hours ago") };

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		sut = new ToItemWriterCommandHandler(itemWriter, itemSearcher);
	}

	@Test
	public void resumeShouldCreateNewItemWithOldComment() {
		// GIVEN
		givenNoCurrentItemIsAvailable();
		DateTime now = DateTime.now();
		TimeTrackingItem item = new TimeTrackingItem("for test",
				now.minusMinutes(20), now.minusMinutes(10));

		// WHEN
		sut.resumeGivenItem(item);

		// THEN
		TimeTrackingItem trackingItem = retrieveWrittenTimeTrackingItem();
		assertThat(trackingItem.getComment().get(), is("for test"));
		assertThat(trackingItem.getStart(),
				is(greaterThan((ReadableInstant) now.minusMinutes(5))));
	}

	@Test
	public void finShouldEndCurrentItem() throws IOException {
		// GIVEN
		TimeTrackingItem unfinished = createUnfinishedItem();
		givenCurrentTimeTrackingItem(unfinished);

		// WHEN
		sut.endCurrentItem();

		// THEN
		assertThatUnfinishedItemWasReplacedByFinished(unfinished);
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
		givenNoCurrentItemIsAvailable();

		// WHEN
		sut.executeCommand("test since 7:00");

		// THEN
		TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
		DateTime start = item.getStart();
		assertThatTimeIsTodayWith(start, 7, 0, 0);
	}

	@Test
	public void shouldParseSince03_12_11() {
		// GIVEN
		givenNoCurrentItemIsAvailable();

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
		givenNoCurrentItemIsAvailable();

		// WHEN
		sut.executeCommand("test since 13:37");

		// THEN
		TimeTrackingItem item = retrieveWrittenTimeTrackingItem();
		DateTime start = item.getStart();
		assertThatTimeIsTodayWith(start, 13, 37, 0);
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
	public void shouldParseFromXtoYWithoutFromCommand() {

		// GIVEN
		DateTime expectedStart = DateTime.now().withTimeAtStartOfDay()
				.withHourOfDay(12);
		DateTime expectedEnd = DateTime.now().withTimeAtStartOfDay()
				.withHourOfDay(13);
		TimeTrackingItem expectedItem = new TimeTrackingItem("comment",
				expectedStart, expectedEnd);
		// WHEN
		Optional<TimeTrackingItem> result = sut
				.executeCommand("comment 12:00 to 13:00");

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
		assertThatUnfinishedItemWasReplacedByFinished(unfinished);
		verifyNoMoreInteractions(itemWriter);
	}

	private TimeTrackingItem createUnfinishedItem() {
		return new TimeTrackingItem(null, DateTime.now().minusMillis(1));
	}

	@Test
	public void shouldWriteCommandsAsNewItems() throws IOException {
		// GIVEN
		givenNoCurrentItemIsAvailable();

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

	private void assertThatUnfinishedItemWasReplacedByFinished(
			TimeTrackingItem unfinished) throws IOException {
		ArgumentCaptor<TimeTrackingItem> newFinishedItemCaptor = ArgumentCaptor
				.forClass(TimeTrackingItem.class);
		verify(itemWriter).replace(eq(unfinished),
				newFinishedItemCaptor.capture());
		TimeTrackingItem nowFinishedItem = newFinishedItemCaptor.getValue();
		assertThat(nowFinishedItem.getEnd(), not(Optional.<DateTime> absent()));
	}

	@Theory
	public void shouldParseMinutesAgoFormats(
			@TestedOn(ints = { 0, 1, 10, 61 }) int minutesAgo, Command format) {
		Assume.assumeTrue(format.isCategory("mins"));
		// GIVEN
		String command = format.supplyCommandFor(minutesAgo);

		// WHEN
		TimeTrackingItem item = retrieveItemWhenCommandIsExecuted(command);

		// THEN
		assertThat("Parameters: '" + minutesAgo + "' '" + command + "'",
				item.getStart(),
				is(Matchers.<ReadableInstant> lessThanOrEqualTo(DateTime.now()
						.minusMinutes(minutesAgo))));
	}

	@Theory
	public void shouldParseSecondsAgoFormats(
			@TestedOn(ints = { 0, 1, 10, 61 }) int secondsAgo, Command format) {
		Assume.assumeTrue(format.isCategory("secs"));
		// GIVEN
		String command = format.supplyCommandFor(secondsAgo);

		// WHEN
		TimeTrackingItem item = retrieveItemWhenCommandIsExecuted(command);

		// THEN
		assertThat("Parameters: '" + secondsAgo + "' '" + command + "'",
				item.getStart(),
				is(Matchers.<ReadableInstant> lessThanOrEqualTo(DateTime.now()
						.minusSeconds(secondsAgo))));
	}

	@Theory
	public void shouldParseHourAgoFormats(
			@TestedOn(ints = { 0, 1, 10, 61 }) int hoursAgo, Command format) {
		Assume.assumeTrue(format.isCategory("hours"));
		// GIVEN
		String command = format.supplyCommandFor(hoursAgo);

		// WHEN
		TimeTrackingItem item = retrieveItemWhenCommandIsExecuted(command);

		// THEN
		assertThat("Parameters: '" + hoursAgo + "' '" + command + "'",
				item.getStart(),
				is(Matchers.<ReadableInstant> lessThanOrEqualTo(DateTime.now()
						.minusHours(hoursAgo))));
	}

	private TimeTrackingItem retrieveItemWhenCommandIsExecuted(String command) {
		givenNoCurrentItemIsAvailable();
		sut.executeCommand(command);
		return retrieveWrittenTimeTrackingItem();
	}

	private TimeTrackingItem retrieveWrittenTimeTrackingItem() {
		ArgumentCaptor<TimeTrackingItem> newTimeTrackingItemCaptor = ArgumentCaptor
				.forClass(TimeTrackingItem.class);
		try {
			verify(itemWriter).write(newTimeTrackingItemCaptor.capture());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		TimeTrackingItem newTimeTrackingItem = newTimeTrackingItemCaptor
				.getValue();
		return newTimeTrackingItem;
	}

	private void givenNoCurrentItemIsAvailable() {
		given(itemSearcher.getCurrentTimeTrackingitem()).willReturn(
				Optional.<TimeTrackingItem> absent());
	}

	private void givenCurrentTimeTrackingItem(TimeTrackingItem item) {
		given(itemSearcher.getCurrentTimeTrackingitem()).willReturn(
				Optional.of(item));
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
