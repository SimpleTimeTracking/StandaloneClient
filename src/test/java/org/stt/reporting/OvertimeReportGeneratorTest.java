package org.stt.reporting;

import java.util.Arrays;
import java.util.Map;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.reporting.ItemCategorizer.ItemCategory;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;

public class OvertimeReportGeneratorTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private Configuration configuration;
	@Mock
	private ItemCategorizer categorizer;
	@Mock
	private ItemReader reader;

	private OvertimeReportGenerator sut;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		// casting to Object is necessary, there is a bug in deep stubbing...
		given((Object) configuration.getDailyWorkingHours().get(anyInt()))
				.willReturn(8);
		given(configuration.getBreakTimeComments()).willReturn(
				Arrays.asList(new String[] { "pause" }));
		given(categorizer.getCategory(anyString())).willReturn(
				ItemCategory.WORKTIME);
		given(categorizer.getCategory("pause")).willReturn(ItemCategory.BREAK);
		sut = new OvertimeReportGenerator(reader, categorizer, configuration);
	}

	@Test
	public void working8hShouldNotProduceOvertime() {

		// GIVEN
		DateTime startTime = DateTime.now().withTimeAtStartOfDay();
		DateTime endTime = startTime.plusHours(8);
		ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime));

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

		// WHEN
		Map<DateTime, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
		assertThat(overtime.values().iterator().next(), is(new Duration(0)));
	}

	@Test
	public void workingOnSaturdayJustIncreasesOvertime() {
		// GIVEN
		// override mock from setUp
		given((Object) configuration.getDailyWorkingHours().get(anyInt()))
				.willReturn(0);
		DateTime startTime = DateTime.now().withTimeAtStartOfDay();
		DateTime endTime = startTime.plusHours(2);
		DateTime breakStartTime = endTime;
		ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime), new TimeTrackingItem("pause",
				breakStartTime, breakStartTime.plusHours(1)));

		// WHEN
		Map<DateTime, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
		assertThat(overtime.values().iterator().next(), is(new Duration(
				2L * 60 * 60 * 1000)));
	}
}
