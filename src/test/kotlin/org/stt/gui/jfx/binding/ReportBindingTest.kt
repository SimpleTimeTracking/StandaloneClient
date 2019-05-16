/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.gui.jfx.binding

import javafx.beans.property.SimpleObjectProperty
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import org.stt.query.TimeTrackingItemQueries
import java.time.Duration
import java.time.LocalDate
import java.util.*
import javax.inject.Provider

/**
 *
 * @author dante
 */
class ReportBindingTest {

    private var sut: ReportBinding? = null
    private val reportStart = SimpleObjectProperty<LocalDate>()
    private val reportEnd = SimpleObjectProperty<LocalDate>()

    private var readerProvider: Provider<ItemReader>? = null
    private val itemReader = mock(ItemReader::class.java)

    @Before
    fun setup() {
        readerProvider = Provider { itemReader }
        sut = ReportBinding(reportStart, reportEnd,
                TimeTrackingItemQueries(readerProvider!!, Optional.empty()))
    }

    @Test
    fun shouldReturnEmptyReportIfStartIsNull() {
        // GIVEN
        reportEnd.set(LocalDate.now())

        // WHEN
        val result = sut!!.value

        // THEN
        assertThat(result.start).isNull()
        assertThat(result.end).isNull()
        assertThat(result.reportingItems).isEmpty()
        assertThat(result.uncoveredDuration).isEqualTo(Duration.ZERO)
    }

    @Test
    fun shouldReturnEmptyReportIfEndIsNull() {
        // GIVEN
        reportStart.set(LocalDate.now())

        // WHEN
        val result = sut!!.value

        // THEN
        assertThat(result.start).isNull()
        assertThat(result.end).isNull()
        assertThat(result.reportingItems).isEmpty()
        assertThat(result.uncoveredDuration).isEqualTo(Duration.ZERO)
    }

    @Test
    fun shouldReadItemsIfStartAndEndAreSet() {
        // GIVEN
        val start = LocalDate.now()
        reportStart.set(start)
        val end = start.plusDays(1)
        reportEnd.set(end)
        val item = TimeTrackingItem("none", start.atStartOfDay(), end.atStartOfDay())
        given<TimeTrackingItem>(itemReader.read()).willReturn(item, null)

        // WHEN
        val result = sut!!.value

        // THEN
        assertThat(result.start).isEqualTo(start.atStartOfDay())
        assertThat(result.end).isEqualTo(end.atStartOfDay())
        assertThat(result.reportingItems).isNotEmpty
        assertThat(result.uncoveredDuration).isEqualTo(Duration.ZERO)
    }
}
