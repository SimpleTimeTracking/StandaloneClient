package org.stt.connector.jira;

import net.rcarz.jiraclient.*;
import org.stt.Service;
import org.stt.config.JiraConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Singleton
public class JiraConnector implements Service {

    private static final Logger LOG = Logger.getLogger(JiraConnector.class.getName());


    private final JiraClient client;
    private Set<String> projectsCache;

    @Inject
    public JiraConnector(JiraConfig configuration) {
        String jiraURI = configuration.getJiraURI();
        if (jiraURI == null || jiraURI.isEmpty()) {
            client = null;
            return;
        }
        try {
            if (configuration.getJiraUsername() != null
                    && !configuration.getJiraUsername().isEmpty()
                    && configuration.getJiraPassword() != null) {
                client = new JiraClient(jiraURI,
                        new BasicCredentials(configuration.getJiraUsername(),
                                new String(configuration.getJiraPassword().getPassword(), "UTF-8")));
            } else {
                client = new JiraClient(jiraURI);
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() {
    }

    public Optional<Issue> getIssue(String issueKey) throws InvalidCredentialsException, IssueDoesNotExistException, AccessDeniedException {
        if (client == null) {
            return Optional.empty();
        }

        String projectKey = getProjectKey(issueKey);

        // Check if the given project key belongs to an existing project
        if (!projectExists(projectKey)) {
            return Optional.empty();
        }

        try {
            Issue jiraIssue = client.getIssue(issueKey);

            return Optional.of(jiraIssue);
        } catch (JiraException e) {
            if (e.getCause() instanceof RestException) {
                RestException cause = (RestException) e.getCause();
                int httpStatusCode = cause.getHttpStatusCode();
                switch (httpStatusCode) {
                    case 404:
                        throw new IssueDoesNotExistException(String.format("Couldn't find issue %s.", issueKey), e);
                    case 401:
                        throw new AccessDeniedException(String.format("You don't have permission to see %s.", issueKey), e);
                }
            }
            throw new JiraConnectorException(String.format("Error while retrieving issue %s: %s", issueKey, e.getCause().getLocalizedMessage()), e);
        }
    }

    private boolean projectExists(String projectKey) throws InvalidCredentialsException {
        return getProjectNames().contains(projectKey);
    }

    private String getProjectKey(String issueKey) {
        int index = issueKey.lastIndexOf('-');

        // Extract the project key
        String projectKey;
        if (index > 0) {
            projectKey = issueKey.substring(0, index);
        } else {
            projectKey = issueKey;
        }
        return projectKey;
    }

    public Set<String> getProjectNames() throws InvalidCredentialsException {
        if (projectsCache == null) {
            projectsCache = internalGetProjectNames();
        }
        return projectsCache;
    }

    private Set<String> internalGetProjectNames() throws InvalidCredentialsException {
        try {
            return client.getProjects().stream()
                    .map(Project::getKey)
                    .collect(Collectors.toSet());
        } catch (JiraException e) {
            if (e.getCause() instanceof RestException) {
                RestException cause = (RestException) e.getCause();
                int httpStatusCode = cause.getHttpStatusCode();
                if (httpStatusCode == 403 || httpStatusCode == 401) {
                    throw new InvalidCredentialsException("Please check your Jira username/password.", e);
                }
            }
            throw new JiraConnectorException(String.format("Error retrieving projects from Jira: %s", e.getLocalizedMessage()), e);
        }
    }
}
