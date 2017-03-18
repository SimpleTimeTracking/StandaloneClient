/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.gui.jfx.binding;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import org.stt.model.TimeTrackingItem;
import org.stt.query.Criteria;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.SummingReportGenerator;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.time.Interval;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class ReportBinding extends ObjectBinding<Report> {

    private final ObservableValue<LocalDate> reportStart;
    private final ObservableValue<LocalDate> reportEnd;
    private final TimeTrackingItemQueries queries;

    public ReportBinding(ObservableValue<LocalDate> reportStart,
                         ObservableValue<LocalDate> reportEnd,
                         TimeTrackingItemQueries queries) {
        this.reportStart = requireNonNull(reportStart);
        this.reportEnd = requireNonNull(reportEnd);
        this.queries = requireNonNull(queries);

		bind(reportStart, reportEnd);
	}

	@Override
	protected Report computeValue() {
		Report report;
		if (reportStart.getValue() != null && reportEnd.getValue() != null) {
			report = createSummaryReportFor();
		} else {
            report = new Report(Collections.emptyList(), null,
                    null, Duration.ZERO);
		}
		return report;
	}

	private Report createSummaryReportFor() {
        Criteria criteria = new Criteria();
        criteria.withStartBetween(Interval.between(reportStart.getValue().atStartOfDay(), reportEnd.getValue().atStartOfDay()));
        try (Stream<TimeTrackingItem> items = queries.queryItems(criteria)) {
            return new SummingReportGenerator(items).createReport();
        }
	}
}
