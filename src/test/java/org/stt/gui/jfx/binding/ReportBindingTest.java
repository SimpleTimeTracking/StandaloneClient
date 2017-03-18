/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.gui.jfx.binding;

import javafx.beans.property.SimpleObjectProperty;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.SummingReportGenerator;

import javax.inject.Provider;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 *
 * @author dante
 */
public class ReportBindingTest {

	private ReportBinding sut;
    private SimpleObjectProperty<LocalDate> reportStart = new SimpleObjectProperty<>();
    private SimpleObjectProperty<LocalDate> reportEnd = new SimpleObjectProperty<>();

    private Provider<ItemReader> readerProvider;
    private ItemReader itemReader = mock(ItemReader.class);

	@Before
	public void setup() {
        readerProvider = () -> itemReader;
        sut = new ReportBinding(reportStart, reportEnd, new TimeTrackingItemQueries(readerProvider, Optional.empty()));
    }

	@Test
	public void shouldReturnEmptyReportIfStartIsNull() {
		// GIVEN
        reportEnd.set(LocalDate.now());

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
        reportStart.set(LocalDate.now());

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
        final LocalDate start = LocalDate.now();
        reportStart.set(start);
        final LocalDate end = start.plusDays(1);
        reportEnd.set(end);
        TimeTrackingItem item = new TimeTrackingItem("none", start.atStartOfDay(), end.atStartOfDay());
        given(itemReader.read()).willReturn(Optional.of(item), Optional.empty());

		// WHEN
		SummingReportGenerator.Report result = sut.getValue();

		// THEN
        Assert.assertThat(result.getStart(), is(start.atStartOfDay()));
        Assert.assertThat(result.getEnd(), is(end.atStartOfDay()));
        Assert.assertThat(result.getReportingItems(), not(IsEmptyCollection.empty()));
		Assert.assertThat(result.getUncoveredDuration(), is(Duration.ZERO));
	}
}
