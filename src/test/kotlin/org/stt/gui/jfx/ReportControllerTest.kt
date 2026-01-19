package org.stt.gui.jfx

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReportNavigationTest {

    @Test
    fun computePreviousTrackedDay_returns_previous() {
        val tracked = setOf(LocalDate.of(2026,1,16), LocalDate.of(2026,1,19))
        val prev = ReportNavigation.computePreviousTrackedDay(LocalDate.of(2026,1,19), tracked)
        assertEquals(LocalDate.of(2026,1,16), prev)
    }

    @Test
    fun computePreviousTrackedDay_returns_null_if_none() {
        val tracked = setOf(LocalDate.of(2026,1,16))
        val prev = ReportNavigation.computePreviousTrackedDay(LocalDate.of(2026,1,16), tracked)
        assertEquals(null, prev)
    }

    @Test
    fun computeNextTrackedDay_returns_next() {
        val tracked = setOf(LocalDate.of(2026,1,16), LocalDate.of(2026,1,19))
        val next = ReportNavigation.computeNextTrackedDay(LocalDate.of(2026,1,16), tracked)
        assertEquals(LocalDate.of(2026,1,19), next)
    }

    @Test
    fun computeNextTrackedDay_returns_null_if_none() {
        val tracked = setOf(LocalDate.of(2026,1,19))
        val next = ReportNavigation.computeNextTrackedDay(LocalDate.of(2026,1,19), tracked)
        assertEquals(null, next)
    }
}
