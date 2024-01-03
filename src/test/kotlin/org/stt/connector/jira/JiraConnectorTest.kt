package org.stt.connector.jira

import org.junit.Test
import org.stt.config.JiraConfig
import org.stt.config.PasswordSetting

internal class JiraConnectorTest {
    @Test
    @Throws(AccessDeniedException::class, InvalidCredentialsException::class, IssueDoesNotExistException::class)
    fun testMissingJiraCredentials() {
        val jiraConfig = JiraConfig()
        jiraConfig.jiraURI = "https://jira.atlassian.net"
        try {
            JiraConnector(jiraConfig)
        } catch (e: Exception) {
            assert(e.message.equals("Credentials missing"))
            assert(e.cause?.message.equals("No username or token configured for Jira connector."))
        }
    }
}
