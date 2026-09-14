package org.stt.connector.jira

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

/**
 * Tests for [JiraClient].
 *
 * Verifies that `getIssue` parses the numeric issue id alongside the summary, that `getMyself`
 * returns the account id, and that error responses are mapped to the typed exceptions, using a
 * local in-process HTTP server instead of a real Jira instance.
 */
internal class JiraClientTest {

  private lateinit var server: HttpServer
  private lateinit var baseUrl: String
  private var issueResponse: Pair<Int, String> = 200 to "{}"
  private var myselfResponse: Pair<Int, String> = 200 to "{}"

  @Before
  fun setup() {
    server = HttpServer.create(InetSocketAddress(0), 0)
    baseUrl = "http://localhost:${server.address.port}/"
    server.createContext("/rest/api/3/issue") { exchange ->
      val (status, body) = issueResponse
      exchange.sendResponseHeaders(status, body.toByteArray().size.toLong())
      exchange.responseBody.use { it.write(body.toByteArray()) }
    }
    server.createContext("/rest/api/3/myself") { exchange ->
      val (status, body) = myselfResponse
      exchange.sendResponseHeaders(status, body.toByteArray().size.toLong())
      exchange.responseBody.use { it.write(body.toByteArray()) }
    }
    server.start()
  }

  @After
  fun tearDown() {
    server.stop(0)
  }

  private fun createClient(): JiraClient = JiraClient("user", "token", baseUrl)

  @Test
  fun shouldReturnSummaryAndNumericIssueId() {
    // GIVEN
    issueResponse = 200 to """{"id": "10042", "fields": {"summary": "Fix the bug"}}"""

    // WHEN
    val issue = createClient().getIssue("JRA-7")

    // THEN
    assertThat(issue).isNotNull()
    assertThat(issue!!.summary).isEqualTo("Fix the bug")
    assertThat(issue.id).isEqualTo(10042L)
  }

  @Test
  fun shouldReturnIssueWithoutIdWhenIdIsMissing() {
    // GIVEN
    issueResponse = 200 to """{"fields": {"summary": "Fix the bug"}}"""

    // WHEN
    val issue = createClient().getIssue("JRA-7")

    // THEN
    assertThat(issue!!.summary).isEqualTo("Fix the bug")
    assertThat(issue.id).isNull()
  }

  @Test
  fun shouldReturnAccountIdFromMyself() {
    // GIVEN
    myselfResponse = 200 to """{"accountId": "5b10ac8d82e05b22cc7d4ef5", "displayName": "Test User"}"""

    // WHEN
    val myself = createClient().getMyself()

    // THEN
    assertThat(myself).isNotNull()
    assertThat(myself!!.accountId).isEqualTo("5b10ac8d82e05b22cc7d4ef5")
  }

  @Test
  fun shouldThrowAccessDeniedForUnauthorizedMyself() {
    // GIVEN
    myselfResponse = 401 to """{"errorMessage": "Unauthorized"}"""

    // WHEN / THEN
    assertThatThrownBy { createClient().getMyself() }
        .isInstanceOf(AccessDeniedException::class.java)
  }

  @Test
  fun shouldThrowInvalidCredentialsForForbiddenMyself() {
    // GIVEN
    myselfResponse = 403 to """{"errorMessage": "Forbidden"}"""

    // WHEN / THEN
    assertThatThrownBy { createClient().getMyself() }
        .isInstanceOf(InvalidCredentialsException::class.java)
  }

  @Test
  fun shouldThrowIssueDoesNotExistForUnknownIssue() {
    // GIVEN
    issueResponse = 404 to """{"errorMessage": "Issue does not exist"}"""

    // WHEN / THEN
    assertThatThrownBy { createClient().getIssue("NOPE-1") }
        .isInstanceOf(IssueDoesNotExistException::class.java)
  }
}
