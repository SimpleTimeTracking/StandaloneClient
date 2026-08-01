package org.stt.submit

import org.stt.model.TimeTrackingItem
import org.stt.reporting.SummingReportGenerator.Report
import org.stt.gui.jfx.ReportController.ReportListItem

interface SubmitConnector {
    val id: String
    fun submitItems(items: List<TimeTrackingItem>)
    fun submitSummary(report: Report, selectedItems: List<ReportListItem>)
}