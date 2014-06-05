package org.stt.importer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import static org.hamcrest.Matchers.*;
import org.junit.Assert;
import org.junit.Test;
import org.stt.importer.ti.DefaultItemExporter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

public class DefaultItemExporterTest {

	@Test (expected = NullPointerException.class)
	public void writeNullObjectFails() throws IOException {
		
		//GIVEN
		Writer stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		
		//WHEN
		theWriter.write(null);
		
		//THEN
		//Exception expected
	}
	
	@Test
	public void writeCommentSucceeds() throws IOException {
		
		//GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		TimeTrackingItem theItem = new TimeTrackingItem("the comment", Calendar.getInstance());
		
		//WHEN
		theWriter.write(theItem);
		
		//THEN
		Assert.assertThat(stringWriter.toString(), containsString("the comment"));
	}
	
	@Test
	public void writeStartSucceeds() throws IOException {
		
		//GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		Calendar theCalendar = Calendar.getInstance();
		//month is 0 based...
		theCalendar.set(2011, 9, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem(null, theCalendar);
		
		//WHEN
		theWriter.write(theItem);
		
		//THEN
		Assert.assertThat(stringWriter.toString(), containsString("2011-10-12_13:14:15"));
	}
	
	@Test
	public void writeEndSucceeds() throws IOException {
		
		//GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		Calendar start = Calendar.getInstance();
		//month is 0 based...
		start.set(2011, 9, 12, 13, 14, 15);
		Calendar end = Calendar.getInstance();
		//month is 0 based...
		end.set(2012, 9, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem(null, start, end);
		
		//WHEN
		theWriter.write(theItem);
		
		//THEN
		Assert.assertThat(stringWriter.toString(), containsString("2012-10-12_13:14:15"));
	}
	
	@Test
	public void writeCompleteEntrySucceeds() throws IOException {
		
		//GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		Calendar start = Calendar.getInstance();
		//month is 0 based...
		start.set(2011, 9, 12, 13, 14, 15);
		Calendar end = Calendar.getInstance();
		//month is 0 based...
		end.set(2012, 9, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem("the comment", start, end);
		
		//WHEN
		theWriter.write(theItem);
		
		//THEN
		Assert.assertThat(stringWriter.toString(), containsString("2011-10-12_13:14:15 2012-10-12_13:14:15 the comment"));
	}
	
	@Test
	public void writeMultiLineEntrySucceeds() throws IOException {
		
		//GIVEN
		StringWriter stringWriter = new StringWriter();
		ItemWriter theWriter = new DefaultItemExporter(stringWriter);
		TimeTrackingItem theItem = new TimeTrackingItem("this is\n a multiline\r string\r\n with different separators", Calendar.getInstance());

		//WHEN
		theWriter.write(theItem);
		
		//THEN
		Assert.assertThat(stringWriter.toString(), endsWith("this is\\n a multiline\\r string\\r\\n with different separators"));
		
	}

}
