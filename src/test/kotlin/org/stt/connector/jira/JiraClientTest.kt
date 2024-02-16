package org.stt.connector.jira

import org.junit.Test
import org.stt.config.JiraConfig

internal class JiraClientTest {

  @Test
  @Throws(AccessDeniedException::class, InvalidCredentialsException::class, IssueDoesNotExistException::class)
  fun testErrorHandlingOfIssueRequest() {
    val jiraConfig = JiraConfig()
    jiraConfig.jiraURI = "https://jira.atlassian.net"
    try {
      JiraClient("dummy", null, jiraConfig.jiraURI!!).getIssue("JRA-7")
    } catch (e: Exception) {
      assert(e.message.equals("Couldn't find issue JRA-7. Cause: {\"errorMessage\": \"Site temporarily unavailable\"}"))
    }
  }
}