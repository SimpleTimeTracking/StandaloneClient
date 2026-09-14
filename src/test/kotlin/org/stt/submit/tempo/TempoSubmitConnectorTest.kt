package org.stt.submit.tempo

import net.engio.mbassy.bus.MBassador
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.willReturn
import org.mockito.kotlin.willThrow
import org.stt.config.TempoConfig
import org.stt.config.TempoSummaryMode
import org.stt.connector.jira.Issue
import org.stt.connector.jira.JiraConnector
import org.stt.connector.jira.Myself
import org.stt.event.NotifyUser
import org.stt.gui.jfx.ReportController.ReportListItem
import org.stt.model.TimeTrackingItem
import org.stt.reporting.SummingReportGenerator.Report
import org.stt.submit.SubmitStatusTracker
import org.stt.time.DurationRounder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * Tests for [TempoSubmitConnector].
 *
 * Verifies the skip rules (no issue key, no end time), the day-level pre-check against existing
 * Tempo worklogs, rounded per-item submission with immediate status marking, the ITEMS and GROUPED
 * summary modes (including synthesized sequential times, ordering by earliest backing item start
 * and per-day restart), the per-run summary notification, single issue resolution per run, and the
 * inert behavior when no Tempo token is configured.
 */
class TempoSubmitConnectorTest {

    private val tempoApiClient: TempoApiClient = mock(TempoApiClient::class.java)
    private val jiraConnector: JiraConnector = mock(JiraConnector::class.java)
    private val submitStatusTracker: SubmitStatusTracker = mock(SubmitStatusTracker::class.java)
    @Suppress("UNCHECKED_CAST")
    private val eventBus: MBassador<Any> = mock(MBassador::class.java) as MBassador<Any>

    private lateinit var tempoConfig: TempoConfig
    private lateinit var sut: TempoSubmitConnector

    @Before
    fun setup() {
        val rounder = DurationRounder()
        rounder.setInterval(Duration.ofMinutes(5))
        tempoConfig = TempoConfig()
        tempoConfig.summaryMode = TempoSummaryMode.ITEMS
        given(jiraConnector.getMyself()).willReturn(Myself("account-1"))
        given(jiraConnector.getIssue(any())).willReturn(Issue("summary", 10042L))
        given(submitStatusTracker.isSubmitted(any(), any())).willReturn(false)
        given(tempoApiClient.getWorklogsForUser(any(), any(), any())).willReturn(emptyList())
        sut = TempoSubmitConnector(tempoConfig, jiraConnector, rounder, submitStatusTracker,
                Optional.of(eventBus), tempoApiClient)
    }

    private fun item(activity: String, day: Int, startHour: Int, startMinute: Int, endHour: Int?, endMinute: Int?): TimeTrackingItem {
        val start = LocalDateTime.of(2026, 9, day, startHour, startMinute, 0)
        val end = if (endHour == null || endMinute == null) null else LocalDateTime.of(2026, 9, day, endHour, endMinute, 0)
        return TimeTrackingItem(activity, start, end)
    }

    private fun emptyReport(): Report = Report(emptyList(), null, null, Duration.ZERO, Duration.ZERO)

    private fun notifications(): List<NotifyUser> {
        val captor = argumentCaptor<Any>()
        verify(eventBus, atLeastOnce()).publish(captor.capture())
        return captor.allValues.filterIsInstance<NotifyUser>()
    }

    @Test
    fun shouldSubmitEachItemWithRoundedTimesAndMarkIt() {
        // GIVEN
        val item = item("ACT-1: fix bug", 14, 8, 2, 9, 1)

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        val captor = argumentCaptor<TempoWorklogInput>()
        verify(tempoApiClient).createWorklog(captor.capture())
        val worklog = captor.firstValue
        assertThat(worklog.authorAccountId).isEqualTo("account-1")
        assertThat(worklog.issueId).isEqualTo(10042L)
        assertThat(worklog.startDate).isEqualTo("2026-09-14")
        assertThat(worklog.startTime).isEqualTo("08:00:00")
        assertThat(worklog.timeSpentSeconds).isEqualTo(3600L)
        verify(submitStatusTracker).markSubmitted(item, "tempo")
    }

    @Test
    fun shouldSkipItemsWithoutIssueKeyWithWarning() {
        // GIVEN
        val item = item("lunch break", 14, 8, 0, 9, 0)

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        verify(tempoApiClient, never()).createWorklog(any())
        assertThat(notifications().any { it.message.contains("no Jira issue key") }).isTrue()
        verify(submitStatusTracker, never()).markSubmitted(any(), any())
    }

    @Test
    fun shouldSkipItemsWithoutEndTime() {
        // GIVEN
        val item = item("ACT-1: ongoing", 14, 8, 0, null, null)

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        verify(tempoApiClient, never()).createWorklog(any())
        verify(submitStatusTracker, never()).markSubmitted(any(), any())
    }

    @Test
    fun shouldSkipItemsWhoseDurationRoundsToZero() {
        // GIVEN a 2-minute item, which rounds to zero with a 5-minute rounding interval
        val item = item("ACT-1: short call", 14, 8, 0, 8, 2)

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        verify(tempoApiClient, never()).createWorklog(any())
        assertThat(notifications().any { it.message.contains("duration rounds to zero") }).isTrue()
        verify(submitStatusTracker, never()).markSubmitted(any(), any())
    }

    @Test
    fun shouldSkipItemsOnDaysWithExistingTempoWorklogs() {
        // GIVEN
        val free = item("ACT-1: fix bug", 14, 8, 0, 9, 0)
        val occupied = item("ACT-1: fix bug", 15, 8, 0, 9, 0)
        given(tempoApiClient.getWorklogsForUser(any(), any(), any()))
                .willReturn(listOf(TempoWorklog(1L, 10042L, "2026-09-15", "08:00:00", 3600L)))

        // WHEN
        sut.submitItems(listOf(free, occupied))

        // THEN
        val captor = argumentCaptor<TempoWorklogInput>()
        verify(tempoApiClient).createWorklog(captor.capture())
        assertThat(captor.firstValue.startDate).isEqualTo("2026-09-14")
        verify(submitStatusTracker).markSubmitted(free, "tempo")
        verify(submitStatusTracker, never()).markSubmitted(eq(occupied), any())
    }

    @Test
    fun shouldMarkOnlySuccessfulItemsOnPartialFailure() {
        // GIVEN
        val first = item("ACT-1: fix bug", 14, 8, 0, 9, 0)
        val second = item("ACT-2: write docs", 14, 9, 0, 10, 0)
        given(tempoApiClient.createWorklog(any()))
                .willReturn(TempoWorklog(1L, 10042L, "2026-09-14", "08:00:00", 3600L))
                .willThrow(TempoClientException("boom"))

        // WHEN
        sut.submitItems(listOf(first, second))

        // THEN
        verify(submitStatusTracker).markSubmitted(first, "tempo")
        verify(submitStatusTracker, never()).markSubmitted(eq(second), any())
        assertThat(notifications().any { it.message.contains("1 submitted, 0 skipped, 1 failed") }).isTrue()
    }

    @Test
    fun shouldNotResubmitAlreadyMarkedItems() {
        // GIVEN
        val item = item("ACT-1: fix bug", 14, 8, 0, 9, 0)
        given(submitStatusTracker.isSubmitted(item, "tempo")).willReturn(true)

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        verify(tempoApiClient, never()).createWorklog(any())
        verify(submitStatusTracker, never()).markSubmitted(any(), any())
    }

    @Test
    fun shouldResolveIssueIdOnlyOncePerRun() {
        // GIVEN
        val items = listOf(
                item("ACT-1: fix bug", 14, 8, 0, 9, 0),
                item("ACT-1: fix more", 14, 10, 0, 11, 0))

        // WHEN
        sut.submitItems(items)

        // THEN
        verify(jiraConnector, times(1)).getIssue("ACT-1")
    }

    @Test
    fun shouldSubmitBackingItemsIndividuallyInItemsMode() {
        // GIVEN
        val first = item("ACT-1: fix bug", 14, 8, 0, 9, 0)
        val second = item("ACT-1: fix more", 14, 10, 0, 11, 0)
        val row = ReportListItem("ACT-1: fix bug", false, Duration.ofHours(2), Duration.ofHours(2), listOf(first, second))

        // WHEN
        sut.submitSummary(emptyReport(), listOf(row))

        // THEN
        val captor = argumentCaptor<TempoWorklogInput>()
        verify(tempoApiClient, times(2)).createWorklog(captor.capture())
        assertThat(captor.allValues.map { it.startTime }).containsExactly("08:00:00", "10:00:00")
        verify(submitStatusTracker).markSubmitted(first, "tempo")
        verify(submitStatusTracker).markSubmitted(second, "tempo")
    }

    @Test
    fun shouldSynthesizeSequentialTimesInGroupedMode() {
        // GIVEN
        tempoConfig.summaryMode = TempoSummaryMode.GROUPED
        tempoConfig.workdayStart = Duration.ofHours(8)
        val first = item("ACT-1: fix bug", 14, 8, 0, 9, 0)
        val second = item("ACT-2: write docs", 14, 9, 0, 10, 0)
        val rowA = ReportListItem("ACT-1: fix bug", false, Duration.ofMinutes(60), Duration.ofMinutes(60), listOf(first))
        val rowB = ReportListItem("ACT-2: write docs", false, Duration.ofMinutes(30), Duration.ofMinutes(30), listOf(second))

        // WHEN
        sut.submitSummary(emptyReport(), listOf(rowA, rowB))

        // THEN
        val captor = argumentCaptor<TempoWorklogInput>()
        verify(tempoApiClient, times(2)).createWorklog(captor.capture())
        assertThat(captor.allValues.map { it.startTime }).containsExactly("08:00:00", "09:00:00")
        assertThat(captor.allValues.map { it.timeSpentSeconds }).containsExactly(3600L, 1800L)
        verify(submitStatusTracker).markSubmitted(first, "tempo")
        verify(submitStatusTracker).markSubmitted(second, "tempo")
    }

    @Test
    fun shouldOrderGroupsByEarliestBackingItemStartInGroupedMode() {
        // GIVEN
        tempoConfig.summaryMode = TempoSummaryMode.GROUPED
        val first = item("ACT-2: write docs", 14, 8, 0, 9, 0)
        val second = item("ACT-1: fix bug", 14, 9, 0, 10, 0)
        val rowA = ReportListItem("ACT-1: fix bug", false, Duration.ofMinutes(60), Duration.ofMinutes(60), listOf(second))
        val rowB = ReportListItem("ACT-2: write docs", false, Duration.ofMinutes(30), Duration.ofMinutes(30), listOf(first))

        // WHEN
        sut.submitSummary(emptyReport(), listOf(rowA, rowB))

        // THEN
        val captor = argumentCaptor<TempoWorklogInput>()
        verify(tempoApiClient, times(2)).createWorklog(captor.capture())
        assertThat(captor.allValues.map { it.startTime }).containsExactly("08:00:00", "08:30:00")
    }

    @Test
    fun shouldRestartSynthesizedTimesPerDayInGroupedMode() {
        // GIVEN
        tempoConfig.summaryMode = TempoSummaryMode.GROUPED
        val day1 = item("ACT-1: fix bug", 14, 8, 0, 10, 0)
        val day2 = item("ACT-1: fix bug", 15, 8, 0, 10, 0)
        val row1 = ReportListItem("ACT-1: fix bug", false, Duration.ofHours(2), Duration.ofHours(2), listOf(day1))
        val row2 = ReportListItem("ACT-1: fix bug", false, Duration.ofHours(2), Duration.ofHours(2), listOf(day2))

        // WHEN
        sut.submitSummary(emptyReport(), listOf(row1, row2))

        // THEN
        val captor = argumentCaptor<TempoWorklogInput>()
        verify(tempoApiClient, times(2)).createWorklog(captor.capture())
        assertThat(captor.allValues.map { it.startDate }).containsExactly("2026-09-14", "2026-09-15")
        assertThat(captor.allValues.map { it.startTime }).containsExactly("08:00:00", "08:00:00")
    }

    @Test
    fun shouldSkipGroupsWithoutIssueKeyInGroupedMode() {
        // GIVEN
        tempoConfig.summaryMode = TempoSummaryMode.GROUPED
        val item = item("ACT-1: fix bug", 14, 8, 0, 9, 0)
        val rowWithKey = ReportListItem("ACT-1: fix bug", false, Duration.ofMinutes(60), Duration.ofMinutes(60), listOf(item))
        val rowWithoutKey = ReportListItem("lunch", false, Duration.ofMinutes(30), Duration.ofMinutes(30), listOf(item))

        // WHEN
        sut.submitSummary(emptyReport(), listOf(rowWithKey, rowWithoutKey))

        // THEN
        val captor = argumentCaptor<TempoWorklogInput>()
        verify(tempoApiClient, times(1)).createWorklog(captor.capture())
        assertThat(notifications().any { it.message.contains("no Jira issue key") }).isTrue()
    }

    @Test
    fun shouldBeInertWithoutConfiguredToken() {
        // GIVEN
        val inert = TempoSubmitConnector(tempoConfig, jiraConnector, DurationRounder(), submitStatusTracker,
                Optional.of(eventBus), null)
        val item = item("ACT-1: fix bug", 14, 8, 0, 9, 0)

        // WHEN
        inert.submitItems(listOf(item))

        // THEN
        verifyNoMoreInteractionsOn(tempoApiClient)
        verifyNoMoreInteractionsOn(jiraConnector)
        assertThat(notifications().any { it.message.contains("not configured") }).isTrue()
        verify(submitStatusTracker, never()).markSubmitted(any(), any())
    }

    private fun verifyNoMoreInteractionsOn(mock: Any) {
        org.mockito.Mockito.verifyNoInteractions(mock)
    }
}
