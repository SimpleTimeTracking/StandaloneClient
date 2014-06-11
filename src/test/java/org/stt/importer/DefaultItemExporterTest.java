package org.stt.importer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

public class DefaultItemExporterTest {

	private StringWriter stringWriter;
	private ItemWriter theWriter;

	@Before
	public void setUp() {
		stringWriter = new StringWriter();

		StreamResourceProvider provider = new StreamResourceProvider() {

			@Override
			public Writer provideTruncatingWriter() throws IOException {
				stringWriter = new StringWriter();
				return stringWriter;
			}

			@Override
			public Reader provideReader() throws FileNotFoundException {
				return new StringReader(stringWriter.toString());
			}

			@Override
			public Writer provideAppendingWriter() throws IOException {
				return stringWriter;
			}

			@Override
			public void close() {
			}
		};

		theWriter = new DefaultItemExporter(provider);
	}

	@Test(expected = NullPointerException.class)
	public void writeNullObjectFails() throws IOException {

		// WHEN
		theWriter.write(null);

		// THEN
		// Exception expected
	}

	@Test
	public void writeCommentSucceeds() throws IOException {

		// GIVEN
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
		TimeTrackingItem theItem = new TimeTrackingItem(
				"this is\n a multiline\r string\r\n with different separators",
				DateTime.now());

		// WHEN
		theWriter.write(theItem);

		// THEN
		Assert.assertThat(
				stringWriter.toString(),
				endsWith("this is\\n a multiline\\r string\\r\\n with different separators"
						+ System.getProperty("line.separator")));

	}

	@Test
	public void itemsCanBeDeleted() throws IOException {

		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("testitem",
				new DateTime(2011, 10, 10, 11, 12, 13));
		TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
				DateTime.now());
		theWriter.write(theItem);
		theWriter.write(theItem2);

		// when
		theWriter.delete(theItem2);

		// then
		Assert.assertThat(
				stringWriter.toString(),
				is("2011-10-10_11:12:13 testitem"
						+ System.getProperty("line.separator")));
	}

	@Test
	public void itemCanBeReplaced() throws IOException {

		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("testitem",
				new DateTime(2011, 10, 10, 11, 12, 13));
		TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
				DateTime.now());
		theWriter.write(theItem2);

		// when
		theWriter.replace(theItem2, theItem);

		// then
		Assert.assertThat(
				stringWriter.toString(),
				is("2011-10-10_11:12:13 testitem"
						+ System.getProperty("line.separator")));
	}

}
