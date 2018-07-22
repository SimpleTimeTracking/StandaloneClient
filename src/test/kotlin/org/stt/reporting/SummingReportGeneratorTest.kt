package org.stt.reporting

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.stt.model.ReportingItem
import org.stt.model.TimeTrackingItem
import java.time.Duration
import java.time.LocalDateTime
import java.util.stream.Stream

class SummingReportGeneratorTest {
    private var sut: SummingReportGenerator? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun shouldSumUpHoles() {
        // GIVEN
        val itemBeforeHole = TimeTrackingItem(
                "item before hole", LocalDateTime.of(2012, 12, 12, 14, 0, 0),
                LocalDateTime.of(2012, 12, 12, 14, 1, 0))
        val itemAfterHole = TimeTrackingItem(
                "item after hole", LocalDateTime.of(2012, 12, 12, 14, 2, 0),
                LocalDateTime.of(2012, 12, 12, 14, 3, 0))

        sut = SummingReportGenerator(Stream.of(itemBeforeHole, itemAfterHole))

        // WHEN
        val report = sut!!.createReport()

        // THEN
        assertThat(report.uncoveredDuration,
                `is`(Duration.ofMinutes(1)))
    }

    @Test
    fun groupingByCommentWorks() {

        // GIVEN
        val expectedItem = TimeTrackingItem("first comment",
                LocalDateTime.of(2012, 12, 12, 14, 14, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 14))
        val expectedItem2 = TimeTrackingItem("first comment",
                LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 16))
        val expectedItem3 = TimeTrackingItem("first comment?",
                LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 17))

        sut = SummingReportGenerator(Stream.of(expectedItem, expectedItem2, expectedItem3))

        // WHEN
        val report = sut!!.createReport()
        val items = report.reportingItems

        // THEN
        Assert.assertThat(items, Matchers.containsInAnyOrder(ReportingItem(
                Duration.ofMillis((60 * 1000 + 2 * 1000).toLong()), "first comment"),
                ReportingItem(Duration.ofMillis((3 * 1000).toLong()), "first comment?")))
    }

    @Test
    fun nullCommentsGetHandledWell() {

        // GIVEN
        val expectedItem = TimeTrackingItem("",
                LocalDateTime.of(2012, 12, 12, 14, 14, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 14))
        val expectedItem2 = TimeTrackingItem("",
                LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 16))

        sut = SummingReportGenerator(Stream.of(expectedItem, expectedItem2))

        // WHEN
        val report = sut!!.createReport()
        val reportingItems = report.reportingItems

        // THEN
        Assert.assertThat(reportingItems, Matchers
                .containsInAnyOrder(ReportingItem(Duration.ofMillis(
                        (60 * 1000 + 2 * 1000).toLong()), "")))
    }

    @Test
    fun reportShouldContainStartOfFirstAndEndOfLastItem() {
        // GIVEN
        val startOfFirstItem = LocalDateTime.of(2012, 12, 12, 14, 14, 14)
        val expectedItem = TimeTrackingItem("",
                startOfFirstItem, LocalDateTime.of(2012, 12, 12, 14, 15, 14))
        val endOfLastItem = LocalDateTime.of(2012, 12, 12, 14, 15, 16)
        val expectedItem2 = TimeTrackingItem("",
                LocalDateTime.of(2012, 12, 12, 14, 15, 14), endOfLastItem)

        sut = SummingReportGenerator(Stream.of(expectedItem, expectedItem2))

        // WHEN
        val report = sut!!.createReport()

        // THEN
        Assert.assertThat<LocalDateTime>(report.start, `is`(startOfFirstItem))
        Assert.assertThat<LocalDateTime>(report.end, `is`(endOfLastItem))
    }
}
