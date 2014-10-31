/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.gui.jfx.binding;

import com.google.common.base.Optional;
import javafx.beans.property.SimpleObjectProperty;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import org.hamcrest.collection.IsEmptyCollection;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.SummingReportGenerator;

/**
 *
 * @author dante
 */
public class ReportBindingTest {

	private ReportBinding sut;
	private SimpleObjectProperty<DateTime> reportStart = new SimpleObjectProperty<>();
	private SimpleObjectProperty<DateTime> reportEnd = new SimpleObjectProperty<>();

	private ItemReaderProvider readerProvider;
	private ItemReader itemReader = mock(ItemReader.class);

	@Before
	public void setup() {
		readerProvider = new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				return itemReader;
			}
		};
		sut = new ReportBinding(reportStart, reportEnd, readerProvider);
	}

	@Test
	public void shouldReturnEmptyReportIfStartIsNull() {
		// GIVEN
		reportEnd.set(new DateTime());

		// WHEN
		SummingReportGenerator.Report result = sut.getValue();

		// THEN
		Assert.assertThat(result.getStart(), CoreMatchers.nullValue());
		Assert.assertThat(result.getEnd(), CoreMatchers.nullValue());
		Assert.assertThat(result.getReportingItems(), IsEmptyCollection.empty());
		Assert.assertThat(result.getUncoveredDuration(), is(Duration.ZERO));
	}

	@Test
	public void shouldReturnEmptyReportIfEndIsNull() {
		// GIVEN
		reportStart.set(new DateTime());

		// WHEN
		SummingReportGenerator.Report result = sut.getValue();

		// THEN
		Assert.assertThat(result.getStart(), CoreMatchers.nullValue());
		Assert.assertThat(result.getEnd(), CoreMatchers.nullValue());
		Assert.assertThat(result.getReportingItems(), IsEmptyCollection.empty());
		Assert.assertThat(result.getUncoveredDuration(), is(Duration.ZERO));
	}

	@Test
	public void shouldReadItemsIfStartAndEndAreSet() {
		// GIVEN
		final DateTime start = new DateTime();
		reportStart.set(start);
		final DateTime end = start.plusDays(1);
		reportEnd.set(end);
		TimeTrackingItem item = new TimeTrackingItem("none", start, end);
		given(itemReader.read()).willReturn(Optional.of(item), Optional.<TimeTrackingItem>absent());

		// WHEN
		SummingReportGenerator.Report result = sut.getValue();

		// THEN
		Assert.assertThat(result.getStart(), is(start));
		Assert.assertThat(result.getEnd(), is(end));
		Assert.assertThat(result.getReportingItems(), not(IsEmptyCollection.empty()));
		Assert.assertThat(result.getUncoveredDuration(), is(Duration.ZERO));
	}
}
