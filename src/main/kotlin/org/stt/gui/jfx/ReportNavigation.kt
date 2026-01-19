package org.stt.gui.jfx

import java.time.LocalDate

internal object ReportNavigation {
    internal fun computePreviousTrackedDay(current: LocalDate, trackedDays: Set<LocalDate>): LocalDate? {
        return trackedDays.filter { it.isBefore(current) }.maxOrNull()
    }

    internal fun computeNextTrackedDay(current: LocalDate, trackedDays: Set<LocalDate>): LocalDate? {
        return trackedDays.filter { it.isAfter(current) }.minOrNull()
    }
}
