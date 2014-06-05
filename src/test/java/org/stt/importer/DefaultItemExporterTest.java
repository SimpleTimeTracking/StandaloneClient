package org.stt.importer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

public class DefaultItemExporterTest {

	@Test(expected = NullPointerException.class)
	public void writeNullObjectFails() throws IOException {

		// GIVEN
		Writer stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);

		// WHEN
		theWriter.write(null);

		// THEN
		// Exception expected
	}

	@Test
	public void writeCommentSucceeds() throws IOException {

		// GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		TimeTrackingItem theItem = new TimeTrackingItem("the comment",
				DateTime.now());

		// WHEN
		theWriter.write(theItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				containsString("the comment"));
	}

	@Test
	public void writeStartSucceeds() throws IOException {

		// GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		DateTime theTime = new DateTime(2011, 10, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem(null, theTime);

		// WHEN
		theWriter.write(theItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				containsString("2011-10-12_13:14:15"));
	}

	@Test
	public void writeEndSucceeds() throws IOException {

		// GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		DateTime start = new DateTime(2011, 10, 12, 13, 14, 15);
		DateTime end = new DateTime(2012, 10, 12, 13, 14, 15);

		TimeTrackingItem theItem = new TimeTrackingItem(null, start, end);

		// WHEN
		theWriter.write(theItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				containsString("2012-10-12_13:14:15"));
	}

	@Test
	public void writeCompleteEntrySucceeds() throws IOException {

		// GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		DateTime start = new DateTime(2011, 10, 12, 13, 14, 15);

		DateTime end = new DateTime(2012, 10, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem("the comment", start,
				end);

		// WHEN
		theWriter.write(theItem);

		// THEN
		Assert.assertThat(
				stringWriter.toString(),
				containsString("2011-10-12_13:14:15 2012-10-12_13:14:15 the comment"));
	}

	@Test
	public void writeMultiLineEntrySucceeds() throws IOException {

		// GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		TimeTrackingItem theItem = new TimeTrackingItem(
				"this is\n a multiline\r string\r\n with different separators",
				DateTime.now());

		// WHEN
		theWriter.write(theItem);

		// THEN
		Assert.assertThat(
				stringWriter.toString(),
				endsWith("this is\\n a multiline\\r string\\r\\n with different separators"));

	}

}
