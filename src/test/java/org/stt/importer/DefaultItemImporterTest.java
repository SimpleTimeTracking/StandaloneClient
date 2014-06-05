package org.stt.importer;

import java.io.StringReader;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class DefaultItemImporterTest {

	@Test
	public void multiLineCommentGetsImportedCorrectly() {

		// GIVEN
		StringReader stringReader = new StringReader(
				"2012-10-10_22:00:00 2012-11-10_22:00:01 this is\\n a multiline\\r string\\r\\n with different separators");
		ItemReader theReader = new DefaultItemImporter(stringReader);

		// WHEN
		Optional<TimeTrackingItem> readItem = theReader.read();

		// THEN
		Assert.assertEquals(
				"this is\n a multiline\r string\r\n with different separators",
				readItem.get().getComment().get());
	}

	@Test
	public void onlyStartTimeGiven() {

		// GIVEN
		StringReader stringReader = new StringReader("2012-10-10_22:00:00");
		ItemReader theReader = new DefaultItemImporter(stringReader);

		// WHEN
		Optional<TimeTrackingItem> readItem = theReader.read();

		// THEN
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
	}
}
