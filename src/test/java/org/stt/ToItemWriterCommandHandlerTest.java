package org.stt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
import org.stt.persistence.ItemSearcher;
import org.stt.persistence.ItemWriter;

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
	public void shouldEndCurrentItemOnFIN() throws IOException {
		// GIVEN
		TimeTrackingItem unfinished = new TimeTrackingItem(null, DateTime.now()
				.minusMillis(1));
		givenCurrentTimeTrackingItem(unfinished);

		// WHEN
		sut.executeCommand("fin");

		// THEN
		assertThatUnfinishedItemWasReplacedByFinished(unfinished);
		verifyNoMoreInteractions(itemWriter);
	}

	@Test
	public void shouldWriteCommandsAsNewItems() throws IOException {
		// GIVEN
		givenNoCurrentItemIsAvailable();

		// WHEN
		sut.executeCommand("test");

		assertThatNewItemWasWritten("test");
	}

	@Test
	public void shouldCloseCurrentlyUnfinishedItem() throws IOException {
		// GIVEN
		TimeTrackingItem unfinished = new TimeTrackingItem(null, DateTime.now()
				.minusMillis(1));
		givenCurrentTimeTrackingItem(unfinished);
		String testComment = "test";

		// WHEN
		sut.executeCommand(testComment);

		// THEN
		assertThatUnfinishedItemWasReplacedByFinished(unfinished);

		assertThatNewItemWasWritten(testComment);
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
