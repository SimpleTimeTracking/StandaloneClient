package org.stt.cli;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.WorkingtimeItemProvider;
import org.stt.text.ItemCategorizer;
import org.stt.text.ItemCategorizer.ItemCategory;
import org.stt.time.DateTimes;

import javax.inject.Provider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

public class ReportPrinterTest {
	private ReportPrinter sut;
    private Provider<ItemReader> readFrom;

	@Mock
	private Configuration configuration;

	@Mock
	private ItemReader itemReader;

	@Mock
	private WorkingtimeItemProvider workingtimeItemProvider;

	@Mock
	private ItemCategorizer categorizer;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

        given(workingtimeItemProvider.getWorkingTimeFor(any(LocalDate.class)))
                .willReturn(new WorkingtimeItemProvider.WorkingtimeItem(Duration.ofHours(8), Duration.ofHours(8)));
        given(configuration.getCliReportingWidth()).willReturn(120);
        readFrom = () -> itemReader;
        given(categorizer.getCategory(anyString())).willReturn(
                ItemCategory.WORKTIME);
        sut = new ReportPrinter(new TimeTrackingItemQueries(readFrom, Optional.empty()), configuration,
                workingtimeItemProvider, categorizer);
    }

	@Test
	public void shouldReportCurrentDayOnNoOptions()
			throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");

        LocalDateTime dateTime = LocalDate.now().atStartOfDay();
        LocalDateTime twoDaysAgo = dateTime.minusDays(2);
        ItemReaderTestHelper.givenReaderReturns(itemReader,
				new TimeTrackingItem("comment", dateTime,
						dateTime.plusHours(2)));
		new TimeTrackingItem("comment yesterday", twoDaysAgo,
				twoDaysAgo.plusHours(1));

		// WHEN
		sut.report(Collections.singleton(""), printStream);

		// THEN
		String result = new String(out.toByteArray(), "UTF8");
		assertThat(result, containsString("comment"));
		assertThat(result, not(containsString("yesterday")));
	}

	@Test
	public void shouldParseSince() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		ItemReaderTestHelper.givenReaderReturns(itemReader,
                new TimeTrackingItem("comment", LocalDateTime.now().minusHours(2),
                        LocalDateTime.now().minusHours(1)));

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
        String expected = DateTimes.prettyPrintDate(LocalDate.now()
                .minusDays(10));
        assertThat(result, containsString(expected));
    }

	@Test
	public void shouldParseAt() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		ItemReaderTestHelper.givenReaderReturns(itemReader,
                new TimeTrackingItem("comment", LocalDateTime.of(2014, 1, 1, 10, 0,
                        0), LocalDateTime.of(2014, 1, 1, 12, 0, 0)));

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
                new TimeTrackingItem("comment blub and stuff", LocalDateTime.now()));

		// WHEN
		sut.report(Collections.singleton("blub"), printStream);

		// THEN
		String result = new String(out.toByteArray(), "UTF8");
		assertThat(result, containsString("comment blub"));
	}

	@Test
	public void shouldParseSearchFilterAllTime()
			throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");

        LocalDateTime twoDaysBefore = LocalDateTime.now().minusDays(2);
        ItemReaderTestHelper.givenReaderReturns(itemReader,
				new TimeTrackingItem("comment blub yesterday", twoDaysBefore,
						twoDaysBefore.plusHours(1)));

		// WHEN
		sut.report(Collections.singleton("blub"), printStream);

		// THEN
		String result = new String(out.toByteArray(), "UTF8");
		assertThat(result, containsString("comment blub yesterday"));
	}

	@Test
	public void shouldParseFromTo() throws UnsupportedEncodingException {
		// GIVEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF8");
		TimeTrackingItem expected1 = new TimeTrackingItem(
                "comment blub and stuff", LocalDateTime.of(2014, 10, 10, 0, 0, 0),
                LocalDateTime.of(2014, 10, 10, 1, 0, 0));
        TimeTrackingItem expected2 = new TimeTrackingItem("other stuff",
                LocalDateTime.of(2014, 10, 11, 0, 0, 0), LocalDateTime.of(2014, 10, 11,
                2, 0, 0));

		ItemReaderTestHelper.givenReaderReturns(itemReader, expected1,
				expected2);

		// WHEN
		sut.report(Collections.singleton("from 2014-10-10 to 2014-10-12"),
				printStream);

		// THEN
		String result = new String(out.toByteArray(), "UTF8");
		assertThat(result, containsString("comment blub"));
		assertThat(result, containsString("other stuff"));
	}
}
