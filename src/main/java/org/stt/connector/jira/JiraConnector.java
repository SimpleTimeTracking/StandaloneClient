package org.stt.connector.jira;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.stt.Configuration;
import org.stt.Service;

import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JiraConnector implements Service {
	
	private static final Logger LOG = Logger.getLogger(JiraConnector.class.getName());
	

	JiraRestClient client = null;
	private Set<String> projectsCache;
	private Configuration configuration;
	private JiraRestClientFactory factory;
	
	@Inject public JiraConnector(Configuration configuration) {
		this(configuration, new AsynchronousJiraRestClientFactory());
	}
	
	public JiraConnector(Configuration configuration, JiraRestClientFactory factory)
	{
		this.configuration = configuration;
		this.factory = factory;
	}


	@Override
	public void start() throws Exception {

		URI jiraURI = configuration.getJiraURI();
		
		if (jiraURI != null)
		{
			AuthenticationHandler authenticationHandler;
			
			String jiraUserName = configuration.getJiraUserName();
			if (jiraUserName != null && jiraUserName.length() > 0)
			{
				authenticationHandler = new BasicHttpAuthenticationHandler(jiraUserName, configuration.getJiraPassword());
			}
			else
			{
				authenticationHandler = new AnonymousAuthenticationHandler();
			}
			
			client = factory.create(jiraURI, authenticationHandler);
		}
	}

	@Override
	public void stop() {
		try {
			if (client != null)
				client.close();
			client = null;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Exception while closing client connection", e);
		}
	}
	
	public Optional<Issue> getIssue(String issueKey) throws JiraConnectorException
	{	
		if (client != null)
		{
			String projectKey = getProjectKey(issueKey);
			
			// Check if the given project key belongs to an existing project
			if (!projectExists(projectKey))
			{
				return Optional.absent();
			}
			
			try 
			{
				IssueRestClient issueClient = client.getIssueClient();
				Promise<Issue> issue = issueClient.getIssue(issueKey);
				
				Issue jiraIssue = issue.get(5000, TimeUnit.MILLISECONDS);
				
				return Optional.of(jiraIssue);
			} 
			catch (InterruptedException e) {
				LOG.log(Level.WARNING, "InterruptedException while retrieving issue", e);
				// Ignore and continue
				return Optional.absent();
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RestClientException)
				{
					handleRestClientException((RestClientException) e.getCause());
				}
				else
				{
					LOG.log(Level.WARNING, "Exception while retrieving issue", e.getCause());
				}
				
				return Optional.absent();
			} catch (TimeoutException e) {
				LOG.log(Level.WARNING, "TimeoutException while retrieving issue", e);
				throw new JiraConnectorException("Connection Timeout", e);
			} catch (RestClientException e) {
				handleRestClientException(e);
				return Optional.absent();
			}
		}
		else
		{
			return Optional.absent();
		}
	}
	
	private void handleRestClientException(RestClientException e) throws JiraConnectorException {
		LOG.log(Level.WARNING, "RestClientException while retrieving issue", e);
		if (e.getStatusCode().isPresent())
		{
			switch (e.getStatusCode().get().intValue())
			{
			
			case HttpURLConnection.HTTP_FORBIDDEN: // 403
				throw new JiraConnectorException("Could not login to Jira, please check your credentials!", e);
			case HttpURLConnection.HTTP_NOT_FOUND: // 404
				// TODO how to discern normal "Http not found" from "Issue not found"?
				// Ignore "Issue not found"
				break;
			default:
				throw new JiraConnectorException("Jira Connector returned exception!", e);
			}
		}
		else
		{
			throw new JiraConnectorException("Jira Connector returned exception!", e);
		}
		
	}

	public Collection<Issue> getIssues(String issueKeyPrefix) throws JiraConnectorException 
	{
		List<Issue> resultList = new ArrayList<>();
		
		if (client != null)
		{
			String projectKey = getProjectKey(issueKeyPrefix);
			
			// Check if the given project key belongs to an existing project
			if (!projectExists(projectKey))
			{
				return Collections.emptyList();
			}
			
			// Get all issue of the project
			SearchRestClient searchClient = client.getSearchClient();
			Promise<SearchResult> searchResultPromise = searchClient.searchJql("project=\"" + projectKey + "\"", Integer.MAX_VALUE, 0, null);
			
			SearchResult searchResult;
			try 
			{
				searchResult = searchResultPromise.get(30, TimeUnit.SECONDS);
			} 
			catch (InterruptedException e) 
			{
				// TODO throw exception Timeout
				LOG.log(Level.WARNING, "Exception while retrieving issues", e);
				return Collections.emptyList();
			} catch (TimeoutException e) {
				LOG.log(Level.WARNING, "Exception while retrieving issues", e);
				throw new JiraConnectorException("Connection Timeout", e);
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RestClientException) {
					handleRestClientException((RestClientException) e.getCause());
				} else {
					LOG.log(Level.WARNING, "Exception while retrieving issue", e.getCause());
				}
				return Collections.emptyList();
			}
			
			Iterable<Issue> issues = searchResult.getIssues();
			
			// Select all issues which start with the given prefix
			
			for (Issue issue : issues)
			{
				if (issue.getKey().startsWith(issueKeyPrefix))
				{
					resultList.add(issue);
				}
			}
		}
		
		return resultList;
	}

	private boolean projectExists(String projectKey) throws JiraConnectorException {
		return getProjectNames().contains(projectKey);
	}

	private String getProjectKey(String issueKey) {
		int index = issueKey.lastIndexOf('-');
		
		// Extract the project key
		String projectKey;
		if (index > 0)
		{
			projectKey = issueKey.substring(0, index);
		}
		else
		{
			projectKey = issueKey;
		}
		return projectKey;
	}
	
	public Set<String> getProjectNames() throws JiraConnectorException
	{
		if (projectsCache == null)
		{
			projectsCache = internalGetProjectNames();
		}
		return projectsCache;
	}

	private Set<String> internalGetProjectNames() throws JiraConnectorException 
	{
		Set<String> projects = new HashSet<>();
		if (client != null)
		{
			ProjectRestClient projectClient = client.getProjectClient();
			
			Promise<Iterable<BasicProject>> projectsPromise = projectClient.getAllProjects();
			
			Iterable<BasicProject> projectsIterable;
			try 
			{
				projectsIterable = projectsPromise.get(5000, TimeUnit.MILLISECONDS);
			} 
			catch (InterruptedException e) {
				LOG.log(Level.WARNING, "Exception while retrieving projects", e);
				return Collections.emptySet();
			} catch (TimeoutException e) {
				LOG.log(Level.WARNING, "Exception while retrieving projects", e);
				throw new JiraConnectorException("Connection Timeout", e);
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RestClientException) {
					handleRestClientException((RestClientException) e.getCause());
				} else {
					LOG.log(Level.WARNING, "Exception while retrieving issue", e.getCause());
				}
				return Collections.emptySet();
			}
			
			for (BasicProject project : projectsIterable)
			{
				projects.add(project.getKey());
			}
		}
		return projects;
	}

}
