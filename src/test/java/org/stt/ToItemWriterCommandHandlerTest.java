package org.stt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemSearcher;
import org.stt.persistence.ItemWriter;

import com.google.common.base.Optional;

public class ToItemWriterCommandHandlerTest {
	@Mock
	ItemWriter itemWriter;

	@Mock
	ItemSearcher itemSearcher;

	private ToItemWriterCommandHandler sut;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		sut = new ToItemWriterCommandHandler(itemWriter, itemSearcher);
	}

	@Test
	public void shouldWriteCommandsAsNewItems() throws IOException {
		// GIVEN
		given(itemSearcher.getCurrentTimeTrackingitem()).willReturn(
				Optional.<TimeTrackingItem> absent());

		// WHEN
		sut.executeCommand("test");

		// THEN
		ArgumentCaptor<TimeTrackingItem> captor = ArgumentCaptor
				.forClass(TimeTrackingItem.class);
		verify(itemWriter).write(captor.capture());
		TimeTrackingItem timeTrackingItem = captor.getValue();
		assertThat(timeTrackingItem.getComment().get(), equalTo("test"));
	}

	@Test
	public void shouldCloseCurrentlyUnfinishedItem() throws IOException {
		// GIVEN
		TimeTrackingItem unfinished = new TimeTrackingItem(null, DateTime.now()
				.minusMillis(1));
		given(itemSearcher.getCurrentTimeTrackingitem()).willReturn(
				Optional.of(unfinished)).willReturn(
				Optional.<TimeTrackingItem> absent());
		String testComment = "test";

		// WHEN
		sut.executeCommand(testComment);

		// THEN
		assertThatUnfinishedItemWasReplacedByFinished(unfinished);

		assertThatNewItemWasWritten(testComment);
	}

	private void assertThatNewItemWasWritten(String testComment)
			throws IOException {
		ArgumentCaptor<TimeTrackingItem> newTimeTrackingItemCaptor = ArgumentCaptor
				.forClass(TimeTrackingItem.class);
		verify(itemWriter).write(newTimeTrackingItemCaptor.capture());
		TimeTrackingItem newTimeTrackingItem = newTimeTrackingItemCaptor
				.getValue();
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
}
