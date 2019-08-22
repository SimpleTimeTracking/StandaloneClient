package org.stt.text

import net.rcarz.jiraclient.Issue
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.stt.connector.jira.JiraConnector
import java.util.*

class JiraExpansionProviderTest {

    @Mock
    internal lateinit var jiraConnector: JiraConnector

    @Mock
    internal lateinit var issue: Issue

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        given(jiraConnector.getIssue("JRA-7")).willReturn(issue)
        given(issue.summary).willReturn("Testing Issue")
    }

    @After
    fun tearDown() {
    }

    @Test
    fun testGetPossibleExpansions() {
        // GIVEN
        val sut = JiraExpansionProvider(jiraConnector, Optional.empty())

        // WHEN
        val matches = sut.getPossibleExpansions("JRA-7")


        // THEN
        assertThat(1).isEqualTo(matches.size.toLong())

        assertThat(matches[0]).isEqualTo(": Testing Issue")
    }

    @Test
    fun testGetPossibleExpansionsShouldHandleSubstring() {
        // GIVEN
        val sut = JiraExpansionProvider(jiraConnector, Optional.empty())

        // WHEN
        val matches = sut.getPossibleExpansions("Test JRA-7")

        // THEN
        assertThat(1).isEqualTo(matches.size.toLong())

        assertThat(matches[0]).isEqualTo(": Testing Issue")
    }

    @Test
    fun testGetPossibleExpansionsShouldHandleSpace() {
        // GIVEN
        val sut = JiraExpansionProvider(jiraConnector, Optional.empty())

        // WHEN
        val matches = sut.getPossibleExpansions(" JRA-7 ")

        // THEN
        assertThat(1).isEqualTo(matches.size.toLong())

        assertThat(matches[0]).isEqualTo(": Testing Issue")
    }

}
