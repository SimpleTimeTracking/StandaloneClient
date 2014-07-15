package org.stt.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.DateTimeHelper;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.WorkingtimeItemProvider;

import static org.hamcrest.CoreMatchers.containsString;

import static org.junit.Assert.assertThat;

import static org.mockito.BDDMockito.given;

public class ReportPrinterTest {
	private ReportPrinter sut;
	@Mock
	private ItemReaderProvider readFrom;

	@Mock
	private Configuration configuration;

	@Mock
	private ItemReader itemReader;

	@Mock
	private WorkingtimeItemProvider workingtimeItemProvider;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		given(configuration.getCliReportingWidth()).willReturn(120);
		given(readFrom.provideReader()).willReturn(itemReader);
		sut = new ReportPrinter(readFrom, configuration,
				workingtimeItemProvider);
	}

	@Test
	public void shouldParseSince() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		ItemReaderTestHelper.givenReaderReturns(itemReader,
				new TimeTrackingItem("comment", DateTime.now().minusHours(2),
						DateTime.now().minusHours(1)));

		// WHEN
		sut.report(Collections.singleton("since 2013-01-01"), printStream);

		// THEN
		String result = new String(out.toByteArray(), "UTF8");
		assertThat(result, containsString("comment"));
	}

	@Test
	public void shouldParseDays() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		ItemReaderTestHelper.givenReaderReturns(itemReader);

		// WHEN
		sut.report(Collections.singleton("10 days"), printStream);

		// THEN
		String result = new String(out.toByteArray(), "UTF8");
		String expected = DateTimeHelper.prettyPrintTime(DateTime.now()
				.minusDays(10));
		assertThat(result, containsString(expected));
	}

	@Test
	public void shouldParseAt() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		ItemReaderTestHelper.givenReaderReturns(itemReader,
				new TimeTrackingItem("comment", new DateTime(2014, 1, 1, 10, 0,
						0), new DateTime(2014, 1, 1, 12, 0, 0)));

		// WHEN
		sut.report(Collections.singleton("at 2014-01-01"), printStream);

		// THEN
		String result = new String(out.toByteArray(), "UTF8");
		assertThat(result, containsString("comment"));
	}

	@Test
	public void shouldParseSearchFilter() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		ItemReaderTestHelper.givenReaderReturns(itemReader,
				new TimeTrackingItem("comment blub and stuff", DateTime.now()));

		// WHEN
		sut.report(Collections.singleton("blub"), printStream);

		// THEN
		String result = new String(out.toByteArray(), "UTF8");
		assertThat(result, containsString("comment blub"));
	}
}
