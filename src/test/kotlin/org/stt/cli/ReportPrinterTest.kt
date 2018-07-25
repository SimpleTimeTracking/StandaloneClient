package org.stt.cli

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.stt.ItemReaderTestHelper
import org.stt.Matchers.any
import org.stt.config.CliConfig
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import org.stt.query.TimeTrackingItemQueries
import org.stt.reporting.WorkingtimeItemProvider
import org.stt.text.ItemCategorizer
import org.stt.text.ItemCategorizer.ItemCategory
import org.stt.time.DateTimes
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.inject.Provider

class ReportPrinterTest {
    private lateinit var sut: ReportPrinter
    private lateinit var readFrom: Provider<ItemReader>

    private val configuration = CliConfig()

    @Mock
    private lateinit var itemReader: ItemReader

    @Mock
    private lateinit var workingtimeItemProvider: WorkingtimeItemProvider

    @Mock
    private lateinit var categorizer: ItemCategorizer

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        given(workingtimeItemProvider.getWorkingTimeFor(any()))
                .willReturn(WorkingtimeItemProvider.WorkingtimeItem(Duration.ofHours(8), Duration.ofHours(8)))
        configuration.cliReportingWidth = 120
        readFrom = Provider { itemReader }
        given(categorizer.getCategory(anyString())).willReturn(
                ItemCategory.WORKTIME)
        sut = ReportPrinter(TimeTrackingItemQueries(readFrom, Optional.empty()), configuration,
                workingtimeItemProvider, categorizer)
    }

    @Test
    fun shouldReportCurrentDayOnNoOptions() {
        // GIVEN
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out, true, StandardCharsets.UTF_8.name())

        val dateTime = LocalDate.now().atStartOfDay()
        val twoDaysAgo = dateTime.minusDays(2)
        ItemReaderTestHelper.givenReaderReturns(itemReader,
                TimeTrackingItem("comment", dateTime,
                        dateTime.plusHours(2)))
        TimeTrackingItem("comment yesterday", twoDaysAgo,
                twoDaysAgo.plusHours(1))

        // WHEN
        sut.report(mutableSetOf(""), printStream)

        // THEN
        val result = String(out.toByteArray(), StandardCharsets.UTF_8)
        assertThat(result, containsString("comment"))
        assertThat(result, not(containsString("yesterday")))
    }

    @Test
    fun shouldParseSince() {
        // GIVEN
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out, true, StandardCharsets.UTF_8.name())
        ItemReaderTestHelper.givenReaderReturns(itemReader,
                TimeTrackingItem("comment", LocalDateTime.now().minusHours(2),
                        LocalDateTime.now().minusHours(1)))

        // WHEN
        sut.report(mutableSetOf("since 2013-01-01"), printStream)

        // THEN
        val result = String(out.toByteArray(), StandardCharsets.UTF_8)
        assertThat(result, containsString("comment"))
    }

    @Test
    fun shouldParseDays() {
        // GIVEN
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out, true, StandardCharsets.UTF_8.name())
        ItemReaderTestHelper.givenReaderReturns(itemReader)

        // WHEN
        sut.report(mutableSetOf("10 days"), printStream)

        // THEN
        val result = String(out.toByteArray(), StandardCharsets.UTF_8)
        val expected = DateTimes.prettyPrintDate(LocalDate.now()
                .minusDays(10))
        assertThat(result, containsString(expected))
    }

    @Test
    fun shouldParseAt() {
        // GIVEN
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out, true, StandardCharsets.UTF_8.name())
        ItemReaderTestHelper.givenReaderReturns(itemReader,
                TimeTrackingItem("comment", LocalDateTime.of(2014, 1, 1, 10, 0,
                        0), LocalDateTime.of(2014, 1, 1, 12, 0, 0)))

        // WHEN
        sut.report(mutableSetOf("at 2014-01-01"), printStream)

        // THEN
        val result = String(out.toByteArray(), StandardCharsets.UTF_8)
        assertThat(result, containsString("comment"))
    }

    @Test
    fun shouldParseSearchFilter() {
        // GIVEN
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out, true, StandardCharsets.UTF_8.name())
        ItemReaderTestHelper.givenReaderReturns(itemReader,
                TimeTrackingItem("comment blub and stuff", LocalDateTime.now()))

        // WHEN
        sut.report(mutableSetOf("blub"), printStream)

        // THEN
        val result = String(out.toByteArray(), StandardCharsets.UTF_8)
        assertThat(result, containsString("comment blub"))
    }

    @Test
    fun shouldParseSearchFilterAllTime() {
        // GIVEN
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out, true, StandardCharsets.UTF_8.name())

        val twoDaysBefore = LocalDateTime.now().minusDays(2)
        ItemReaderTestHelper.givenReaderReturns(itemReader,
                TimeTrackingItem("comment blub yesterday", twoDaysBefore,
                        twoDaysBefore.plusHours(1)))

        // WHEN
        sut.report(mutableSetOf("blub"), printStream)

        // THEN
        val result = String(out.toByteArray(), StandardCharsets.UTF_8)
        assertThat(result, containsString("comment blub yesterday"))
    }

    @Test
    fun shouldParseFromTo() {
        // GIVEN
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out, true, StandardCharsets.UTF_8.name())
        val expected1 = TimeTrackingItem(
                "comment blub and stuff", LocalDateTime.of(2014, 10, 10, 0, 0, 0),
                LocalDateTime.of(2014, 10, 10, 1, 0, 0))
        val expected2 = TimeTrackingItem("other stuff",
                LocalDateTime.of(2014, 10, 11, 0, 0, 0), LocalDateTime.of(2014, 10, 11,
                2, 0, 0))

        ItemReaderTestHelper.givenReaderReturns(itemReader, expected1,
                expected2)

        // WHEN
        sut.report(mutableSetOf("from 2014-10-10 to 2014-10-12"),
                printStream)

        // THEN
        val result = String(out.toByteArray(), StandardCharsets.UTF_8)
        assertThat(result, containsString("comment blub"))
        assertThat(result, containsString("other stuff"))
    }
}
