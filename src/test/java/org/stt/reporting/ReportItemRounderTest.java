/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.reporting;

import java.util.Arrays;
import java.util.Map;
import org.hamcrest.Matchers;
import org.joda.time.Duration;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.stt.model.ReportingItem;
import org.stt.time.DurationRounder;

/**
 *
 * @author dante
 */
public class ReportItemRounderTest {

	private ReportItemRounder sut;

	@Before
	public void setup() {
		DurationRounder durationRounder = new DurationRounder();
		durationRounder.setInterval(Duration.standardMinutes(10));
		sut = new ReportItemRounder(durationRounder);
	}

	@Test
	public void shouldMapItemsToRoundedDurations() {
		// GIVEN
		ReportingItem item1 = new ReportingItem(Duration.standardMinutes(3), "item 1");
		ReportingItem item2 = new ReportingItem(Duration.standardMinutes(12), "item 2");
		ReportingItem item3 = new ReportingItem(Duration.standardMinutes(17), "item 3");

		// WHEN
		Map<ReportingItem, Duration> result = sut.mapFromReportItemToRoundedDuration(Arrays.asList(item1, item2, item3));

		// THEN
		assertThat(result, Matchers.hasEntry(item1, Duration.standardMinutes(0)));
		assertThat(result, Matchers.hasEntry(item2, Duration.standardMinutes(10)));
		assertThat(result, Matchers.hasEntry(item3, Duration.standardMinutes(20)));
	}

}
