package org.stt.submit.tempo

import net.engio.mbassy.bus.MBassador
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.argumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.stt.Matchers.any
import org.stt.Matchers.argThat
import org.stt.config.TempoConfig
import org.stt.connector.jira.Issue
import org.stt.connector.jira.JiraConnector
import org.stt.connector.jira.Myself
import org.stt.model.TimeTrackingItem
import org.stt.submit.SubmitStatusTracker
import org.stt.time.DurationRounder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * Verifies the submission behavior of the Tempo connector: case-insensitive
 * issue key extraction, using the text after the issue key as worklog
 * comment, and the per-issue-and-day guard against double submission (only
 * the same issue on the same day blocks; other issues submit normally).
 */
class TempoSubmitConnectorTest {

    private lateinit var sut: TempoSubmitConnector
    private lateinit var jiraConnector: JiraConnector
    private lateinit var tempoApiClient: TempoApiClient
    private lateinit var submitStatusTracker: SubmitStatusTracker

    private val day = LocalDate.of(2026, 9, 17)

    @Before
    fun setup() {
        jiraConnector = mock(JiraConnector::class.java)
        tempoApiClient = mock(TempoApiClient::class.java)
        submitStatusTracker = mock(SubmitStatusTracker::class.java)
        given(jiraConnector.getMyself()).willReturn(Myself("account-1"))
        given(jiraConnector.getIssue("DEVTT-6")).willReturn(Issue(summary = "test issue", id = 42))
        given(jiraConnector.getIssue("OTHER-1")).willReturn(Issue(summary = "other issue", id = 99))
        given(tempoApiClient.getWorklogsForUser(anyString(), any<java.time.LocalDate>(), any<java.time.LocalDate>())).willReturn(emptyList())
        sut = TempoSubmitConnector(TempoConfig(), jiraConnector, DurationRounder(),
                submitStatusTracker, Optional.empty<MBassador<Any>>(), tempoApiClient)
    }

    @Test
    fun shouldExtractIssueKeyCaseInsensitivelyAndNormalizeToUpperCase() {
        // GIVEN / WHEN / THEN
        assertThat(TempoSubmitConnector.extractIssueKey("devtt-6: fix bug")).isEqualTo("DEVTT-6")
        assertThat(TempoSubmitConnector.extractIssueKey("Devtt-6")).isEqualTo("DEVTT-6")
        assertThat(TempoSubmitConnector.extractIssueKey("DEVTT-6 fix bug")).isEqualTo("DEVTT-6")
        assertThat(TempoSubmitConnector.extractIssueKey("no issue here")).isNull()
    }

    @Test
    fun shouldExtractCommentAfterIssueKey() {
        // GIVEN / WHEN / THEN
        assertThat(TempoSubmitConnector.extractWorklogComment("DEVTT-6: fix the bug")).isEqualTo("fix the bug")
        assertThat(TempoSubmitConnector.extractWorklogComment("devtt-6 - fix the bug")).isEqualTo("fix the bug")
        assertThat(TempoSubmitConnector.extractWorklogComment("DEVTT-6 fix the bug")).isEqualTo("fix the bug")
        assertThat(TempoSubmitConnector.extractWorklogComment("DEVTT-6")).isNull()
    }

    @Test
    fun shouldSubmitLowercaseKeyedItemWithCommentFromActivityText() {
        // GIVEN
        val item = TimeTrackingItem("devtt-6: fix the login bug",
                day.atTime(20, 38, 34), day.atTime(22, 0, 0))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        val captor = argumentCaptor<TempoWorklogInput>()
        verify(tempoApiClient).createWorklog(captor.capture())
        val worklog = captor.firstValue
        assertThat(worklog.issueId).isEqualTo(42)
        assertThat(worklog.description).isEqualTo("fix the login bug")
        assertThat(worklog.startDate).isEqualTo(day.toString())
        verify(submitStatusTracker).markSubmitted(item, "tempo")
    }

    @Test
    fun shouldSkipItemWhenSameIssueAlreadyLoggedOnSameDay() {
        // GIVEN
        val item = TimeTrackingItem("DEVTT-6: fix the login bug",
                day.atTime(20, 38, 34), day.atTime(22, 0, 0))
        given(tempoApiClient.getWorklogsForUser(anyString(), any<java.time.LocalDate>(), any<java.time.LocalDate>()))
                .willReturn(listOf(TempoWorklog(tempoWorklogId = 1, issueId = 42, startDate = day.toString(),
                        startTime = null, timeSpentSeconds = 3600)))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        verify(tempoApiClient, never()).createWorklog(any())
        verify(submitStatusTracker, never()).markSubmitted(item, "tempo")
    }

    @Test
    fun shouldSubmitDifferentIssueOnDayWithWorklogsOfOtherIssues() {
        // GIVEN - another issue has worklogs on that day
        val item = TimeTrackingItem("DEVTT-6: fix the login bug",
                day.atTime(20, 38, 34), day.atTime(22, 0, 0))
        given(tempoApiClient.getWorklogsForUser(anyString(), any<java.time.LocalDate>(), any<java.time.LocalDate>()))
                .willReturn(listOf(TempoWorklog(tempoWorklogId = 1, issueId = 99, startDate = day.toString(),
                        startTime = null, timeSpentSeconds = 3600)))

        // WHEN
        sut.submitItems(listOf(item))

        // THEN
        verify(tempoApiClient).createWorklog(argThat { it.issueId == 42L })
        verify(submitStatusTracker).markSubmitted(item, "tempo")
    }
}
