/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.gui.jfx.binding

import javafx.beans.binding.ObjectBinding
import javafx.beans.value.ObservableValue
import org.stt.query.Criteria
import org.stt.query.TimeTrackingItemQueries
import org.stt.reporting.SummingReportGenerator
import org.stt.reporting.SummingReportGenerator.Report
import org.stt.time.until
import java.time.Duration
import java.time.LocalDate

class ReportBinding(val reportStart: ObservableValue<LocalDate>,
                    val reportEnd: ObservableValue<LocalDate>,
                    val queries: TimeTrackingItemQueries) : ObjectBinding<Report>() {

    init {
        bind(reportStart, reportEnd)
    }

    override fun computeValue() =
            if (reportStart.value !=
                    null && reportEnd.value != null) {
                createSummaryReportFor()
            } else {
                Report(emptyList(), null, null, Duration.ZERO)
            }

    private fun createSummaryReportFor(): Report {
        val criteria = Criteria()
        criteria.withStartBetween(reportStart.value.atStartOfDay() until reportEnd.value.atStartOfDay())
        queries.queryItems(criteria).use { items -> return SummingReportGenerator(items).createReport() }
    }
}
