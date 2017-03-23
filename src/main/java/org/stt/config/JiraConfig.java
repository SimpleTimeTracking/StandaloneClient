package org.stt.config;

public class JiraConfig implements ConfigurationContainer {
    private String jiraURI;
    private String jiraUsername;
    private PasswordSetting jiraPassword;

    public String getJiraURI() {
        return jiraURI;
    }

    public void setJiraURI(String jiraURI) {
        this.jiraURI = jiraURI;
    }

    public String getJiraUsername() {
        return jiraUsername;
    }

    public void setJiraUsername(String jiraUsername) {
        this.jiraUsername = jiraUsername;
    }

    public PasswordSetting getJiraPassword() {
        return jiraPassword;
    }

    public void setJiraPassword(PasswordSetting jiraPassword) {
        this.jiraPassword = jiraPassword;
    }
}
