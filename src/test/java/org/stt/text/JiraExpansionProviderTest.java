package org.stt.text;

import net.rcarz.jiraclient.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.connector.jira.JiraConnector;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

public class JiraExpansionProviderTest {
	
	@Mock
	JiraConnector jiraConnector;
	
	@Mock
	Issue issue;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		given(jiraConnector.getIssue("JRA-7")).willReturn(Optional.of(issue));
		given(issue.getSummary()).willReturn("Testing Issue");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetPossibleExpansions() {
		// GIVEN
		JiraExpansionProvider sut = new JiraExpansionProvider(jiraConnector, Optional.empty());
		
		// WHEN
		List<String> matches = sut.getPossibleExpansions("JRA-7");
		
		
		// THEN
		assertEquals(1, matches.size());
		
		assertEquals(": Testing Issue", matches.get(0));
	}
	
	@Test
	public void testGetPossibleExpansionsShouldHandleSubstring() {
		// GIVEN
		JiraExpansionProvider sut = new JiraExpansionProvider(jiraConnector, Optional.empty());
		
		// WHEN
		List<String> matches = sut.getPossibleExpansions("Test JRA-7");
		
		// THEN
		assertEquals(1, matches.size());
		
		assertEquals(": Testing Issue", matches.get(0));
	}
	
	@Test
	public void testGetPossibleExpansionsShouldHandleSpace() {
		// GIVEN
		JiraExpansionProvider sut = new JiraExpansionProvider(jiraConnector, Optional.empty());
		
		// WHEN
		List<String> matches = sut.getPossibleExpansions(" JRA-7 ");
		
		// THEN
		assertEquals(1, matches.size());
		
		assertEquals(": Testing Issue", matches.get(0));
	}

}
