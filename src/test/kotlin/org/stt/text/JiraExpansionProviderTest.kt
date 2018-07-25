package org.stt.text

import net.rcarz.jiraclient.Issue
import org.junit.After
import org.junit.Assert.assertEquals
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

        given(jiraConnector.getIssue("JRA-7")).willReturn(Optional.of(issue))
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
        assertEquals(1, matches.size.toLong())

        assertEquals(": Testing Issue", matches[0])
    }

    @Test
    fun testGetPossibleExpansionsShouldHandleSubstring() {
        // GIVEN
        val sut = JiraExpansionProvider(jiraConnector, Optional.empty())

        // WHEN
        val matches = sut.getPossibleExpansions("Test JRA-7")

        // THEN
        assertEquals(1, matches.size.toLong())

        assertEquals(": Testing Issue", matches[0])
    }

    @Test
    fun testGetPossibleExpansionsShouldHandleSpace() {
        // GIVEN
        val sut = JiraExpansionProvider(jiraConnector, Optional.empty())

        // WHEN
        val matches = sut.getPossibleExpansions(" JRA-7 ")

        // THEN
        assertEquals(1, matches.size.toLong())

        assertEquals(": Testing Issue", matches[0])
    }

}
