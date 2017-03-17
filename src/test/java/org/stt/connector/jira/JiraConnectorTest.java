package org.stt.connector.jira;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;

import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.util.concurrent.Promise;
import com.google.common.base.Optional;

public class JiraConnectorTest {

	@Mock
	private Configuration configuration;
	
	private JiraConnector sut;

	@Mock
	private JiraRestClientFactory factory;

	@Mock
	private JiraRestClient restClient;

	@Mock
	private IssueRestClient issueClient;

	@Mock
	private ProjectRestClient projectClient;

	@Mock
	private SearchRestClient searchClient;
	
	@Before
	public void setUp() throws Exception {
		
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getJiraURI()).willReturn(new URI("https://jira.atlassian.com/"));
		given(configuration.getJiraUserName()).willReturn("");
		
		given(factory.create(any(URI.class), any(AuthenticationHandler.class))).willReturn(restClient);
		
		given(restClient.getIssueClient()).willReturn(issueClient);
		given(restClient.getProjectClient()).willReturn(projectClient);
		given(restClient.getSearchClient()).willReturn(searchClient);
		
		sut = new JiraConnector(configuration, factory);
		
		
	}

	@After
	public void tearDown() throws Exception {
		sut.stop();
	}

	@Test
	public void testConnection() throws Exception {		
		// GIVEN
		sut.start();
		
		@SuppressWarnings("unchecked")
		Promise<Iterable<BasicProject>> projectPromise = Mockito.mock(Promise.class);
		
		List<BasicProject> projectList = new ArrayList<>();
		projectList.add(new BasicProject(new URI("https://jira.atlassian.com"), "Project1Key", 1L, "Project1"));
		projectList.add(new BasicProject(new URI("https://jira.atlassian.com"), "Project2Key", 2L, "Project2"));
		
		given(projectPromise.get(anyLong(), any(TimeUnit.class))).willReturn(projectList);
		
		given(projectClient.getAllProjects()).willReturn(projectPromise);
		
		// WHEN
		Set<String> prefixes = sut.getProjectNames();
		
		// THEN
		assertNotNull(prefixes);
		assertEquals(2, prefixes.size());
		assertTrue(prefixes.contains("Project1Key"));
		assertTrue(prefixes.contains("Project2Key"));
	}
	
	@Test
	public void testGetIssue() throws Exception
	{
		// GIVEN
		sut.start();
		
		@SuppressWarnings("unchecked")
		Promise<Iterable<BasicProject>> projectPromise = Mockito.mock(Promise.class);
		
		List<BasicProject> projectList = new ArrayList<>();
		projectList.add(new BasicProject(new URI("https://jira.atlassian.com"), "JRA", 1L, "JRA"));
		
		given(projectPromise.get(anyLong(), any(TimeUnit.class))).willReturn(projectList);
		
		given(projectClient.getAllProjects()).willReturn(projectPromise);
		
		@SuppressWarnings("unchecked")
		Promise<Issue> issuePromise = Mockito.mock(Promise.class);
		
		Issue issueInject = new Issue("Test", new URI("https://jira.atlassion.com"), "JRA-1", 15L, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null);
		given(issuePromise.get(anyLong(), any(TimeUnit.class))).willReturn(issueInject);
		
		given(issueClient.getIssue("JRA-1")).willReturn(issuePromise);
		
		// WHEN
		Optional<Issue> issue = sut.getIssue("JRA-1");
		
		// THEN
		assertTrue(issue.isPresent());
		assertEquals(issueInject, issue.get());
	}
	
}
