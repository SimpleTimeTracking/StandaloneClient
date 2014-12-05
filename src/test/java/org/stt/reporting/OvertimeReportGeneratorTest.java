package org.stt.reporting;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.ItemReaderTestHelper;
import org.stt.analysis.ItemCategorizer;
import org.stt.analysis.ItemCategorizer.ItemCategory;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

public class OvertimeReportGeneratorTest {

	@Mock
	private Configuration configuration;
	@Mock
	private ItemCategorizer categorizer;
	@Mock
	private ItemReader reader;
	@Mock
	private WorkingtimeItemProvider workingtimeItemProvider;

	private OvertimeReportGenerator sut;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		given(configuration.getBreakTimeComments()).willReturn(
				Arrays.asList(new String[] { "pause" }));
		given(categorizer.getCategory(anyString())).willReturn(
				ItemCategory.WORKTIME);
		given(categorizer.getCategory("pause")).willReturn(ItemCategory.BREAK);

		sut = new OvertimeReportGenerator(reader, categorizer,
				workingtimeItemProvider);
	}

	@Test
	public void working8hShouldNotProduceOvertime() {

		// GIVEN
		DateTime startTime = DateTime.now().withTimeAtStartOfDay();
		DateTime endTime = startTime.plusHours(8);
		ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime));

		Duration toReturn = new Duration(8 * DateTimeConstants.MILLIS_PER_HOUR);
		given(workingtimeItemProvider.getWorkingTimeFor(startTime)).willReturn(
				new WorkingtimeItem(toReturn, toReturn));

		// WHEN
		Map<DateTime, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
		assertThat(overtime.values().iterator().next(), is(new Duration(0)));
	}

	@Test
	public void working8hWithBreaksShouldNotProduceOvertime() {

		// GIVEN
		DateTime startTime = DateTime.now().withTimeAtStartOfDay();
		DateTime endTime = startTime.plusHours(8);
		DateTime breakStartTime = endTime;
		ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime), new TimeTrackingItem("pause",
				breakStartTime, breakStartTime.plusHours(3)));

		Duration toReturn = new Duration(8 * DateTimeConstants.MILLIS_PER_HOUR);
		given(workingtimeItemProvider.getWorkingTimeFor(startTime)).willReturn(
				new WorkingtimeItem(toReturn, toReturn));

		// WHEN
		Map<DateTime, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
		assertThat(overtime.values().iterator().next(), is(new Duration(0)));
	}

	@Test
	public void workingOnSaturdayJustIncreasesOvertime() {

		// GIVEN
		DateTime startTime = new DateTime(2014, 07, 05, 0, 0, 0);
		DateTime endTime = startTime.plusHours(2);
		DateTime breakStartTime = endTime;
		ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime), new TimeTrackingItem("pause",
				breakStartTime, breakStartTime.plusHours(1)));

		Duration toReturn = new Duration(0);
		given(workingtimeItemProvider.getWorkingTimeFor(startTime)).willReturn(
				new WorkingtimeItem(toReturn, toReturn));

		// WHEN
		Map<DateTime, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
		assertThat(overtime.values().iterator().next(), is(new Duration(
				2L * DateTimeConstants.MILLIS_PER_HOUR)));
	}

	@Test
	public void dayOf14WorkhoursShouldProduceNegativeOvertime() {
		// GIVEN
		DateTime startTime = new DateTime(2014, 1, 1, 0, 0, 0);
		DateTime endTime = startTime.plusHours(2);
		DateTime breakStartTime = endTime;
		ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime), new TimeTrackingItem("pause",
				breakStartTime, breakStartTime.plusHours(1)));

		Duration toReturn = new Duration(14 * DateTimeConstants.MILLIS_PER_HOUR);
		given(workingtimeItemProvider.getWorkingTimeFor(startTime)).willReturn(
				new WorkingtimeItem(toReturn, toReturn));

		// WHEN
		Map<DateTime, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
		assertThat(overtime.values().iterator().next(), is(new Duration(-12L
				* DateTimeConstants.MILLIS_PER_HOUR)));
	}
}
