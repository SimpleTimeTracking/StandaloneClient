package org.stt;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Calendar;

import org.junit.Test;
import org.stt.persistence.ItemWriter;

public class ToItemWriterCommandHandlerTest {
	@Test
	public void shouldWriteCommandsAsNewItems() {
		// GIVEN
		ItemWriter itemWriter = mock(ItemWriter.class);
		ToItemWriterCommandHandler sut = new ToItemWriterCommandHandler(
				itemWriter);

		// WHEN
		sut.executeCommand("test");

		// THEN
		verify(itemWriter).writeItemAt(any(Calendar.class), eq("test"));
	}

}
