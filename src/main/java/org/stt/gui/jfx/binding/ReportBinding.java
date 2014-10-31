/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.gui.jfx.binding;

import java.io.IOException;
import java.util.Collections;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.filter.StartDateReaderFilter;
import org.stt.model.ReportingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.SummingReportGenerator;
import org.stt.reporting.SummingReportGenerator.Report;

import com.google.common.base.Preconditions;

/**
 *
 * @author dante
 */
public class ReportBinding extends ObjectBinding<Report> {

	private final ObservableValue<DateTime> reportStart;
	private final ObservableValue<DateTime> reportEnd;
	private final ItemReaderProvider readerProvider;

	public ReportBinding(ObservableValue<DateTime> reportStart,
			ObservableValue<DateTime> reportEnd,
			ItemReaderProvider readerProvider) {
		Preconditions.checkNotNull(reportStart);
		Preconditions.checkNotNull(reportEnd);
		Preconditions.checkNotNull(readerProvider);

		this.reportStart = reportStart;
		this.reportEnd = reportEnd;
		this.readerProvider = readerProvider;

		bind(reportStart, reportEnd);
	}

	@Override
	protected Report computeValue() {
		Report report;
		if (reportStart.getValue() != null && reportEnd.getValue() != null) {
			report = createSummaryReportFor();
		} else {
			report = new Report(Collections.<ReportingItem> emptyList(), null,
					null, Duration.ZERO);
		}
		return report;
	}

	private Report createSummaryReportFor() {
		try (ItemReader itemReader = readerProvider.provideReader();
				StartDateReaderFilter filter = new StartDateReaderFilter(
						itemReader, reportStart.getValue(),
						reportEnd.getValue())) {
			SummingReportGenerator reportGenerator = new SummingReportGenerator(
					filter);
			return reportGenerator.createReport();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
