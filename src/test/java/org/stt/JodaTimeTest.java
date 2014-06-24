package org.stt;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

public class JodaTimeTest {
	@Test
	public void shouldBeAbleToParsePrintedFormat() {
		DateTimeFormatter pattern = DateTimeFormat
				.forPattern("yyyy.MM.dd HH:mm:ss");
		DateTime dateTime = DateTime.now().withMillis(0);
		String asString = pattern.print(dateTime);
		DateTime parsedTime = pattern.parseDateTime(asString);
		assertThat(parsedTime, is(dateTime));
	}
}
