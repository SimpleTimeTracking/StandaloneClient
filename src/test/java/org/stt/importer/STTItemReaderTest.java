package org.stt.importer;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.stt.STTItemReader;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Optional;

public class STTItemReaderTest {

	@Test
	public void multiLineCommentGetsImportedCorrectly() {

		// GIVEN
		StringReader stringReader = new StringReader(
				"2012-10-10_22:00:00 2012-11-10_22:00:01 this is\\n a multiline\\r string\\r\\n with different separators");
		ItemReader theReader = new STTItemReader(stringReader);

		// WHEN
		Optional<TimeTrackingItem> readItem = theReader.read();

		// THEN
		Assert.assertEquals(
				"this is\n a multiline\r string\r\n with different separators",
                readItem.get().getActivity());
    }

	@Test
	public void onlyStartTimeGiven() {

		// GIVEN
		StringReader stringReader = new StringReader("2012-10-10_22:00:00");
		ItemReader theReader = new STTItemReader(stringReader);

		// WHEN
		Optional<TimeTrackingItem> readItem = theReader.read();

		// THEN
        LocalDateTime time = LocalDateTime.of(2012, 10, 10, 22, 00, 00);
        Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
	}

	@Test
	public void startTimeAndCommentGiven() {

		// GIVEN
		StringReader stringReader = new StringReader(
				"2012-10-10_22:00:00 the long comment");
		ItemReader theReader = new STTItemReader(stringReader);

		// WHEN
		Optional<TimeTrackingItem> readItem = theReader.read();

		// THEN
        LocalDateTime time = LocalDateTime.of(2012, 10, 10, 22, 00, 00);
        Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
		Assert.assertThat("the long comment",
                Matchers.equalTo(readItem.get().getActivity()));
    }
}
