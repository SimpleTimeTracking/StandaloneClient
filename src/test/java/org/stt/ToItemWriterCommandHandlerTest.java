package org.stt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

public class ToItemWriterCommandHandlerTest {
	@Test
	public void shouldWriteCommandsAsNewItems() throws IOException {
		// GIVEN
		ItemWriter itemWriter = mock(ItemWriter.class);
		ToItemWriterCommandHandler sut = new ToItemWriterCommandHandler(
				itemWriter);

		// WHEN
		sut.executeCommand("test");

		// THEN
		ArgumentCaptor<TimeTrackingItem> captor = ArgumentCaptor
				.forClass(TimeTrackingItem.class);
		verify(itemWriter).write(captor.capture());
		TimeTrackingItem timeTrackingItem = captor.getValue();
		assertThat(timeTrackingItem.getComment().get(), equalTo("test"));
	}

}
