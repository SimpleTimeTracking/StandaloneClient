package org.stt.cli;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.ItemReaderTestHelper;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.WorkingtimeItemProvider;

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
		given(readFrom.provideReader()).willReturn(itemReader);
		sut = new ReportPrinter(readFrom, configuration,
				workingtimeItemProvider);
	}

	@Test
	public void shouldParseSince() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		ItemReaderTestHelper.givenReaderReturns(itemReader);

		// WHEN
		sut.report(Collections.singleton("since 2013-01-01"), printStream);

		// THEN
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
		assertThat(result, containsString("10 days"));
	}

	@Test
	public void shouldParseSearchFilter() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		ItemReaderTestHelper.givenReaderReturns(itemReader);

		// WHEN
		sut.report(Collections.singleton("blub"), printStream);

		// THEN

	}
}
