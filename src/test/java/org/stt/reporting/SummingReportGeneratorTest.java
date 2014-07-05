package org.stt.reporting;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.ItemReaderTestHelper;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.reporting.SummingReportGenerator.Report;

public class SummingReportGeneratorTest {

	@Mock
	ItemReader reader;
	private SummingReportGenerator sut;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		sut = new SummingReportGenerator(reader);
	}

	@Test
	public void shouldSumUpHoles() {
		// GIVEN
		TimeTrackingItem itemBeforeHole = new TimeTrackingItem(
				"item before hole", new DateTime(2012, 12, 12, 14, 0, 0),
				new DateTime(2012, 12, 12, 14, 1, 0));
		TimeTrackingItem itemAfterHole = new TimeTrackingItem(
				"item after hole", new DateTime(2012, 12, 12, 14, 2, 0),
				new DateTime(2012, 12, 12, 14, 3, 0));

		ItemReaderTestHelper.givenReaderReturns(reader, itemBeforeHole,
				itemAfterHole);
		// WHEN
		Report report = sut.createReport();

		// THEN
		assertThat(report.getUncoveredDuration(),
				is(Duration.standardMinutes(1)));
	}

	@Test
	public void groupingByCommentWorks() {

		// GIVEN
		TimeTrackingItem expectedItem = new TimeTrackingItem("first comment",
				new DateTime(2012, 12, 12, 14, 14, 14), new DateTime(2012, 12,
						12, 14, 15, 14));
		TimeTrackingItem expectedItem2 = new TimeTrackingItem("first comment",
				new DateTime(2012, 12, 12, 14, 15, 14), new DateTime(2012, 12,
						12, 14, 15, 16));
		TimeTrackingItem expectedItem3 = new TimeTrackingItem("first comment?",
				new DateTime(2012, 12, 12, 14, 15, 14), new DateTime(2012, 12,
						12, 14, 15, 17));

		ItemReaderTestHelper.givenReaderReturns(reader, expectedItem,
				expectedItem2, expectedItem3);

		// WHEN
		Report report = sut.createReport();
		List<ReportingItem> items = report.getReportingItems();

		// THEN
		Assert.assertThat(items, Matchers.containsInAnyOrder(new ReportingItem(
				new Duration(60 * 1000 + 2 * 1000), "first comment"),
				new ReportingItem(new Duration(3 * 1000), "first comment?")));
	}

	@Test
	public void nullCommentsGetHandledWell() {

		// GIVEN
		TimeTrackingItem expectedItem = new TimeTrackingItem(null,
				new DateTime(2012, 12, 12, 14, 14, 14), new DateTime(2012, 12,
						12, 14, 15, 14));
		TimeTrackingItem expectedItem2 = new TimeTrackingItem(null,
				new DateTime(2012, 12, 12, 14, 15, 14), new DateTime(2012, 12,
						12, 14, 15, 16));

		ItemReaderTestHelper.givenReaderReturns(reader, expectedItem,
				expectedItem2);

		// WHEN
		Report report = sut.createReport();
		List<ReportingItem> reportingItems = report.getReportingItems();

		// THEN
		Assert.assertThat(reportingItems, Matchers
				.containsInAnyOrder(new ReportingItem(new Duration(
						60 * 1000 + 2 * 1000), "")));
	}

	@Test
	public void reportShouldContainStartOfFirstAndEndOfLastItem() {
		// GIVEN
		DateTime startOfFirstItem = new DateTime(2012, 12, 12, 14, 14, 14);
		TimeTrackingItem expectedItem = new TimeTrackingItem(null,
				startOfFirstItem, new DateTime(2012, 12, 12, 14, 15, 14));
		DateTime endOfLastItem = new DateTime(2012, 12, 12, 14, 15, 16);
		TimeTrackingItem expectedItem2 = new TimeTrackingItem(null,
				new DateTime(2012, 12, 12, 14, 15, 14), endOfLastItem);

		ItemReaderTestHelper.givenReaderReturns(reader, expectedItem,
				expectedItem2);

		// WHEN
		Report report = sut.createReport();

		// THEN
		Assert.assertThat(report.getStart(), is(startOfFirstItem));
		Assert.assertThat(report.getEnd(), is(endOfLastItem));
	}
}
