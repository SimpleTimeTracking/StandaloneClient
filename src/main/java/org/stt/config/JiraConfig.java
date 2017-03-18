package org.stt.config;

import java.net.URI;

public class JiraConfig {
    private URI jiraURI;
    private String jiraUsername;
    private String jiraPassword;

    public URI getJiraURI() {
        return jiraURI;
    }

    public void setJiraURI(URI jiraURI) {
        this.jiraURI = jiraURI;
    }

    public String getJiraUsername() {
        return jiraUsername;
    }

    public void setJiraUsername(String jiraUsername) {
        this.jiraUsername = jiraUsername;
    }

    public String getJiraPassword() {
        return jiraPassword;
    }

    public void setJiraPassword(String jiraPassword) {
        this.jiraPassword = jiraPassword;
    }
}
