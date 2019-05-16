package org.stt.reporting

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.stt.ItemReaderTestHelper
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import org.stt.query.TimeTrackingItemQueries
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem
import org.stt.text.ItemCategorizer
import org.stt.text.ItemCategorizer.ItemCategory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.inject.Provider

class OvertimeReportGeneratorTest {

    @Mock
    private lateinit var categorizer: ItemCategorizer
    @Mock
    private lateinit var reader: ItemReader
    private lateinit var itemReaderProvider: Provider<ItemReader>
    @Mock
    private lateinit var workingtimeItemProvider: WorkingtimeItemProvider

    private lateinit var sut: OvertimeReportGenerator
    private lateinit var queries: TimeTrackingItemQueries

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        given(categorizer.getCategory(anyString())).willReturn(
                ItemCategory.WORKTIME)
        given(categorizer.getCategory("pause")).willReturn(ItemCategory.BREAK)
        itemReaderProvider = Provider { reader }
        queries = TimeTrackingItemQueries(itemReaderProvider, Optional.empty())

        sut = OvertimeReportGenerator(queries, categorizer,
                workingtimeItemProvider)
    }

    @Test
    fun working8hShouldNotProduceOvertime() {

        // GIVEN
        val startTime = LocalDate.now().atStartOfDay()
        val endTime = startTime.plusHours(8)
        ItemReaderTestHelper.givenReaderReturns(reader, TimeTrackingItem(
                "working", startTime, endTime))

        val toReturn = Duration.ofHours(8)
        given(workingtimeItemProvider.getWorkingTimeFor(startTime.toLocalDate())).willReturn(
                WorkingtimeItem(toReturn, toReturn))

        // WHEN
        val overtime = sut.overtime

        // THEN
        assertThat(overtime.entries).hasSize(1)
        assertThat(overtime.values).first().isEqualTo(Duration.ZERO)
    }

    @Test
    fun working8hWithBreaksShouldNotProduceOvertime() {

        // GIVEN
        val startTime = LocalDate.now().atStartOfDay()
        val endTime = startTime.plusHours(8)
        ItemReaderTestHelper.givenReaderReturns(reader, TimeTrackingItem(
                "working", startTime, endTime), TimeTrackingItem("pause",
                endTime, endTime.plusHours(3)))

        val toReturn = Duration.ofHours(8)
        given(workingtimeItemProvider.getWorkingTimeFor(startTime.toLocalDate())).willReturn(
                WorkingtimeItem(toReturn, toReturn))

        // WHEN
        val overtime = sut.overtime

        // THEN
        assertThat(overtime.entries).hasSize(1)
        assertThat(overtime.values).first().isEqualTo(Duration.ZERO)
    }

    @Test
    fun workingOnSaturdayJustIncreasesOvertime() {

        // GIVEN
        val startTime = LocalDateTime.of(2014, 7, 5, 0, 0, 0)
        val endTime = startTime.plusHours(2)
        ItemReaderTestHelper.givenReaderReturns(reader, TimeTrackingItem(
                "working", startTime, endTime), TimeTrackingItem("pause",
                endTime, endTime.plusHours(1)))

        val toReturn = Duration.ZERO
        given(workingtimeItemProvider.getWorkingTimeFor(startTime.toLocalDate())).willReturn(
                WorkingtimeItem(toReturn, toReturn))

        // WHEN
        val overtime = sut.overtime

        // THEN
        assertThat(overtime.entries).hasSize(1)
        assertThat(overtime.values).first().isEqualTo(Duration.ofHours(2))
    }

    @Test
    fun dayOf14WorkhoursShouldProduceNegativeOvertime() {
        // GIVEN
        val startTime = LocalDateTime.of(2014, 1, 1, 0, 0, 0)
        val endTime = startTime.plusHours(2)
        ItemReaderTestHelper.givenReaderReturns(reader, TimeTrackingItem(
                "working", startTime, endTime), TimeTrackingItem("pause",
                endTime, endTime.plusHours(1)))

        val toReturn = Duration.ofHours(14)
        given(workingtimeItemProvider.getWorkingTimeFor(startTime.toLocalDate())).willReturn(
                WorkingtimeItem(toReturn, toReturn))

        // WHEN
        val overtime = sut.overtime

        // THEN
        assertThat(overtime.entries).hasSize(1)
        assertThat(overtime.values).first().isEqualTo(Duration.ofHours(-12L))
    }
}
