package org.stt.reporting;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.reporting.ItemCategorizer.ItemCategory;

import com.google.common.base.Optional;

public class OvertimeReportGeneratorTest {

	@Mock
	private Configuration configuration;
	@Mock
	private ItemCategorizer categorizer;
	@Mock
	private ItemReader reader;

	private OvertimeReportGenerator sut;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		given(configuration.getDailyWorkingHours()).willReturn(8);
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
		DateTime startTime = DateTime.now();
		DateTime endTime = startTime.plusHours(8);
		given(reader.read())
				.willReturn(
						Optional.of(new TimeTrackingItem("working", startTime,
								endTime))).willReturn(
						Optional.<TimeTrackingItem> absent());
		// WHEN
		Duration overtime = sut.getOvertime();

		// THEN
		assertThat(overtime, is(new Duration(0)));
	}

	@Test
	public void working8hWithBreaksShouldNotProduceOvertime() {

		// GIVEN
		DateTime startTime = DateTime.now();
		DateTime endTime = startTime.plusHours(8);
		given(reader.read())
				.willReturn(
						Optional.of(new TimeTrackingItem("working", startTime,
								endTime)))
				.willReturn(
						Optional.of(new TimeTrackingItem("pause", DateTime
								.now(), DateTime.now().plusHours(3))))
				.willReturn(Optional.<TimeTrackingItem> absent());
		// WHEN
		Duration overtime = sut.getOvertime();

		// THEN
		assertThat(overtime, is(new Duration(0)));
	}
}
