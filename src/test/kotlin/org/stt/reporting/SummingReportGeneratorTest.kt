package org.stt.reporting

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import org.stt.model.ReportingItem
import org.stt.model.TimeTrackingItem
import org.stt.text.ItemCategorizer
import org.stt.time.DurationRounder
import java.time.Duration
import java.time.LocalDateTime
import java.util.stream.Stream

class SummingReportGeneratorTest {
    private var closeable: AutoCloseable? = null
    private var sut: SummingReportGenerator? = null

    @Mock
    private lateinit var itemCategorizer: ItemCategorizer;

    @Mock
    private lateinit var rounder: DurationRounder

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        given(itemCategorizer.getCategory(anyString())).willReturn(ItemCategorizer.ItemCategory.WORKTIME)
        given(rounder.roundDuration(anyOrNull())).willReturn(Duration.ofDays(1));
    }

    @After
    fun after() {
        closeable?.close();
    }

    @Test
    fun shouldSumUpHoles() {
        // GIVEN
        val itemBeforeHole = TimeTrackingItem(
            "item before hole", LocalDateTime.of(2012, 12, 12, 14, 0, 0),
            LocalDateTime.of(2012, 12, 12, 14, 1, 0)
        )
        val itemAfterHole = TimeTrackingItem(
            "item after hole", LocalDateTime.of(2012, 12, 12, 14, 2, 0),
            LocalDateTime.of(2012, 12, 12, 14, 3, 0)
        )

        sut = SummingReportGenerator(Stream.of(itemBeforeHole, itemAfterHole), itemCategorizer, rounder)

        // WHEN
        val report = sut!!.createReport()

        // THEN
        assertThat(report.uncoveredDuration).isEqualTo(Duration.ofMinutes(1))
    }

    @Test
    fun groupingByCommentWorks() {

        // GIVEN
        val expectedItem = TimeTrackingItem(
            "first comment",
            LocalDateTime.of(2012, 12, 12, 14, 14, 14), LocalDateTime.of(
                2012, 12,
                12, 14, 15, 14
            )
        )
        val expectedItem2 = TimeTrackingItem(
            "first comment",
            LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(
                2012, 12,
                12, 14, 15, 16
            )
        )
        val expectedItem3 = TimeTrackingItem(
            "first comment?",
            LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(
                2012, 12,
                12, 14, 15, 17
            )
        )

        sut = SummingReportGenerator(Stream.of(expectedItem, expectedItem2, expectedItem3), itemCategorizer, rounder)

        // WHEN
        val report = sut!!.createReport()
        val items = report.reportingItems

        // THEN
        assertThat(items).contains(
            ReportingItem(
                Duration.ofMillis((60 * 1000 + 2 * 1000).toLong()), Duration.ofDays(1), "first comment", false
            ),
            ReportingItem(Duration.ofMillis((3 * 1000).toLong()), Duration.ofDays(1), "first comment?", false)
        )
    }

    @Test
    fun nullCommentsGetHandledWell() {

        // GIVEN
        val expectedItem = TimeTrackingItem(
            "",
            LocalDateTime.of(2012, 12, 12, 14, 14, 14), LocalDateTime.of(
                2012, 12,
                12, 14, 15, 14
            )
        )
        val expectedItem2 = TimeTrackingItem(
            "",
            LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(
                2012, 12,
                12, 14, 15, 16
            )
        )

        sut = SummingReportGenerator(Stream.of(expectedItem, expectedItem2), itemCategorizer, rounder)

        // WHEN
        val report = sut!!.createReport()
        val reportingItems = report.reportingItems

        // THEN
        assertThat(reportingItems).contains(
            ReportingItem(
                Duration.ofMillis(
                    (60 * 1000 + 2 * 1000).toLong()
                ), Duration.ofDays(1), "", false
            )
        )
    }

    @Test
    fun reportShouldContainStartOfFirstAndEndOfLastItem() {
        // GIVEN
        val startOfFirstItem = LocalDateTime.of(2012, 12, 12, 14, 14, 14)
        val expectedItem = TimeTrackingItem(
            "",
            startOfFirstItem, LocalDateTime.of(2012, 12, 12, 14, 15, 14)
        )
        val endOfLastItem = LocalDateTime.of(2012, 12, 12, 14, 15, 16)
        val expectedItem2 = TimeTrackingItem(
            "",
            LocalDateTime.of(2012, 12, 12, 14, 15, 14), endOfLastItem
        )

        sut = SummingReportGenerator(Stream.of(expectedItem, expectedItem2), itemCategorizer, rounder)

        // WHEN
        val report = sut!!.createReport()

        // THEN
        assertThat(report.start).isEqualTo(startOfFirstItem)
        assertThat(report.end).isEqualTo(endOfLastItem)
    }
}
