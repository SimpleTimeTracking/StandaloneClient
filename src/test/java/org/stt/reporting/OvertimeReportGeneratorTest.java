package org.stt.reporting;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem;
import org.stt.text.ItemCategorizer;
import org.stt.text.ItemCategorizer.ItemCategory;

import javax.inject.Provider;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

public class OvertimeReportGeneratorTest {

	@Mock
	private ItemCategorizer categorizer;
	@Mock
	private ItemReader reader;
    private Provider<ItemReader> itemReaderProvider;
    @Mock
    private WorkingtimeItemProvider workingtimeItemProvider;

	private OvertimeReportGenerator sut;
    private TimeTrackingItemQueries queries;

    @Before
    public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

        given(categorizer.getCategory(anyString())).willReturn(
				ItemCategory.WORKTIME);
		given(categorizer.getCategory("pause")).willReturn(ItemCategory.BREAK);
        itemReaderProvider = () -> reader;
        queries = new TimeTrackingItemQueries(itemReaderProvider, Optional.empty());

        sut = new OvertimeReportGenerator(queries, categorizer,
                workingtimeItemProvider);
	}

	@Test
	public void working8hShouldNotProduceOvertime() {

		// GIVEN
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = startTime.plusHours(8);
        ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime));

        Duration toReturn = Duration.ofHours(8);
        given(workingtimeItemProvider.getWorkingTimeFor(startTime.toLocalDate())).willReturn(
				new WorkingtimeItem(toReturn, toReturn));

		// WHEN
        Map<LocalDate, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
        assertThat(overtime.values().iterator().next(), is(Duration.ZERO));
    }

	@Test
	public void working8hWithBreaksShouldNotProduceOvertime() {

		// GIVEN
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = startTime.plusHours(8);
        LocalDateTime breakStartTime = endTime;
        ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime), new TimeTrackingItem("pause",
				breakStartTime, breakStartTime.plusHours(3)));

        Duration toReturn = Duration.ofHours(8);
        given(workingtimeItemProvider.getWorkingTimeFor(startTime.toLocalDate())).willReturn(
				new WorkingtimeItem(toReturn, toReturn));

		// WHEN
        Map<LocalDate, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
        assertThat(overtime.values().iterator().next(), is(Duration.ZERO));
    }

	@Test
	public void workingOnSaturdayJustIncreasesOvertime() {

		// GIVEN
        LocalDateTime startTime = LocalDateTime.of(2014, 07, 05, 0, 0, 0);
        LocalDateTime endTime = startTime.plusHours(2);
        LocalDateTime breakStartTime = endTime;
        ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime), new TimeTrackingItem("pause",
				breakStartTime, breakStartTime.plusHours(1)));

        Duration toReturn = Duration.ZERO;
        given(workingtimeItemProvider.getWorkingTimeFor(startTime.toLocalDate())).willReturn(
				new WorkingtimeItem(toReturn, toReturn));

		// WHEN
        Map<LocalDate, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
        assertThat(overtime.values().iterator().next(), is(Duration.ofHours(2)));
    }

	@Test
	public void dayOf14WorkhoursShouldProduceNegativeOvertime() {
		// GIVEN
        LocalDateTime startTime = LocalDateTime.of(2014, 1, 1, 0, 0, 0);
        LocalDateTime endTime = startTime.plusHours(2);
        LocalDateTime breakStartTime = endTime;
        ItemReaderTestHelper.givenReaderReturns(reader, new TimeTrackingItem(
				"working", startTime, endTime), new TimeTrackingItem("pause",
				breakStartTime, breakStartTime.plusHours(1)));

        Duration toReturn = Duration.ofHours(14);
        given(workingtimeItemProvider.getWorkingTimeFor(startTime.toLocalDate())).willReturn(
				new WorkingtimeItem(toReturn, toReturn));

		// WHEN
        Map<LocalDate, Duration> overtime = sut.getOvertime();

		// THEN
		assertThat(overtime.entrySet(), Matchers.hasSize(1));
        assertThat(overtime.values().iterator().next(), is(Duration.ofHours(-12L)));
    }
}
