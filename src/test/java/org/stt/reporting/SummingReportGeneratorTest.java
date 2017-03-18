package org.stt.reporting;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.reporting.SummingReportGenerator.Report;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SummingReportGeneratorTest {
	private SummingReportGenerator sut;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldSumUpHoles() {
		// GIVEN
		TimeTrackingItem itemBeforeHole = new TimeTrackingItem(
                "item before hole", LocalDateTime.of(2012, 12, 12, 14, 0, 0),
                LocalDateTime.of(2012, 12, 12, 14, 1, 0));
        TimeTrackingItem itemAfterHole = new TimeTrackingItem(
                "item after hole", LocalDateTime.of(2012, 12, 12, 14, 2, 0),
                LocalDateTime.of(2012, 12, 12, 14, 3, 0));

        sut = new SummingReportGenerator(Stream.of(itemBeforeHole, itemAfterHole));

		// WHEN
		Report report = sut.createReport();

		// THEN
		assertThat(report.getUncoveredDuration(),
                is(Duration.ofMinutes(1)));
    }

	@Test
	public void groupingByCommentWorks() {

		// GIVEN
		TimeTrackingItem expectedItem = new TimeTrackingItem("first comment",
                LocalDateTime.of(2012, 12, 12, 14, 14, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 14));
		TimeTrackingItem expectedItem2 = new TimeTrackingItem("first comment",
                LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 16));
		TimeTrackingItem expectedItem3 = new TimeTrackingItem("first comment?",
                LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 17));

        sut = new SummingReportGenerator(Stream.of(expectedItem, expectedItem2, expectedItem3));

		// WHEN
		Report report = sut.createReport();
		List<ReportingItem> items = report.getReportingItems();

		// THEN
		Assert.assertThat(items, Matchers.containsInAnyOrder(new ReportingItem(
                        Duration.ofMillis(60 * 1000 + 2 * 1000), "first comment"),
                new ReportingItem(Duration.ofMillis(3 * 1000), "first comment?")));
    }

	@Test
	public void nullCommentsGetHandledWell() {

		// GIVEN
        TimeTrackingItem expectedItem = new TimeTrackingItem("",
                LocalDateTime.of(2012, 12, 12, 14, 14, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 14));
        TimeTrackingItem expectedItem2 = new TimeTrackingItem("",
                LocalDateTime.of(2012, 12, 12, 14, 15, 14), LocalDateTime.of(2012, 12,
                12, 14, 15, 16));

        sut = new SummingReportGenerator(Stream.of(expectedItem, expectedItem2));

		// WHEN
		Report report = sut.createReport();
		List<ReportingItem> reportingItems = report.getReportingItems();

		// THEN
		Assert.assertThat(reportingItems, Matchers
                .containsInAnyOrder(new ReportingItem(Duration.ofMillis(
                        60 * 1000 + 2 * 1000), "")));
	}

	@Test
	public void reportShouldContainStartOfFirstAndEndOfLastItem() {
		// GIVEN
        LocalDateTime startOfFirstItem = LocalDateTime.of(2012, 12, 12, 14, 14, 14);
        TimeTrackingItem expectedItem = new TimeTrackingItem("",
                startOfFirstItem, LocalDateTime.of(2012, 12, 12, 14, 15, 14));
        LocalDateTime endOfLastItem = LocalDateTime.of(2012, 12, 12, 14, 15, 16);
        TimeTrackingItem expectedItem2 = new TimeTrackingItem("",
                LocalDateTime.of(2012, 12, 12, 14, 15, 14), endOfLastItem);

        sut = new SummingReportGenerator(Stream.of(expectedItem, expectedItem2));

		// WHEN
		Report report = sut.createReport();

		// THEN
		Assert.assertThat(report.getStart(), is(startOfFirstItem));
		Assert.assertThat(report.getEnd(), is(endOfLastItem));
	}
}
