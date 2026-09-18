package org.stt.submit.tempo

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.time.LocalDate

/**
 * Tests for [TempoApiClient].
 *
 * Verifies the request shaping of worklog creation and worklog queries, the parsing of the
 * returned worklogs, and the mapping of HTTP error responses to the typed exceptions, using a
 * local in-process HTTP server instead of the real Tempo API.
 */
class TempoApiClientTest {

    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private var createResponse: Pair<Int, String> = 200 to "{}"
    private var listResponse: Pair<Int, String> = 200 to "{}"
    private val receivedBodies = mutableListOf<String>()
    private val receivedPaths = mutableListOf<String>()
    private val receivedAuthHeaders = mutableListOf<String>()

    @Before
    fun setup() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        baseUrl = "http://localhost:${server.address.port}/4"
        server.createContext("/4/worklogs") { exchange ->
            receivedPaths.add(exchange.requestURI.toString())
            exchange.requestHeaders.getFirst("Authorization")?.let { receivedAuthHeaders.add(it) }
            when (exchange.requestMethod) {
                "POST" -> {
                    receivedBodies.add(exchange.requestBody.readBytes().toString(Charsets.UTF_8))
                    val (status, body) = createResponse
                    exchange.sendResponseHeaders(status, body.toByteArray().size.toLong())
                    exchange.responseBody.use { it.write(body.toByteArray()) }
                }
                else -> {
                    val (status, body) = listResponse
                    exchange.sendResponseHeaders(status, body.toByteArray().size.toLong())
                    exchange.responseBody.use { it.write(body.toByteArray()) }
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    private fun createClient(): TempoApiClient = TempoApiClient(baseUrl, "tempo-token")

    @Test
    fun shouldSendWorklogWithAllRequiredFields() {
        // GIVEN
        createResponse = 200 to """{"id": "4711", "issue": {"id": "10042"}, "startDate": "2026-09-14",
            "startTime": "08:00:00", "timeSpentSeconds": 3600}"""
        val input = TempoWorklogInput("account-1", 10042L, "2026-09-14", "08:00:00", 3600L)

        // WHEN
        val worklog = createClient().createWorklog(input)

        // THEN
        assertThat(receivedBodies).hasSize(1)
        assertThat(receivedBodies[0]).contains("\"authorAccountId\": \"account-1\"")
            .contains("\"issueId\": 10042")
            .contains("\"startDate\": \"2026-09-14\"")
            .contains("\"startTime\": \"08:00:00\"")
            .contains("\"timeSpentSeconds\": 3600")
        assertThat(receivedPaths[0]).isEqualTo("/4/worklogs")
        assertThat(receivedAuthHeaders).containsExactly("Bearer tempo-token")
        assertThat(worklog.tempoWorklogId).isEqualTo(4711L)
        assertThat(worklog.issueId).isEqualTo(10042L)
        assertThat(worklog.startDate).isEqualTo("2026-09-14")
        assertThat(worklog.startTime).isEqualTo("08:00:00")
        assertThat(worklog.timeSpentSeconds).isEqualTo(3600L)
    }

    @Test
    fun shouldQueryWorklogsOfUserWithDateRange() {
        // GIVEN
        listResponse = 200 to """{"metadata": {"count": 1}, "results": [
            {"id": "1", "issue": {"id": "11"}, "startDate": "2026-09-14", "startTime": "08:00:00", "timeSpentSeconds": 1800}]}"""

        // WHEN
        val worklogs = createClient().getWorklogsForUser("account-1", LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 14))

        // THEN
        assertThat(receivedPaths[0]).isEqualTo("/4/worklogs/user/account-1?from=2026-09-14&to=2026-09-14&limit=1000")
        assertThat(receivedAuthHeaders).containsExactly("Bearer tempo-token")
        assertThat(worklogs).hasSize(1)
        assertThat(worklogs[0].tempoWorklogId).isEqualTo(1L)
        assertThat(worklogs[0].issueId).isEqualTo(11L)
        assertThat(worklogs[0].startDate).isEqualTo("2026-09-14")
        assertThat(worklogs[0].timeSpentSeconds).isEqualTo(1800L)
    }

    @Test
    fun shouldReturnEmptyListWhenNoResults() {
        // GIVEN
        listResponse = 200 to """{"metadata": {"count": 0}, "results": []}"""

        // WHEN
        val worklogs = createClient().getWorklogsForUser("account-1", LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 15))

        // THEN
        assertThat(worklogs).isEmpty()
    }

    @Test
    fun shouldMapUnauthorizedToAccessDenied() {
        // GIVEN
        createResponse = 401 to """{"errors": [{"message": "Unauthorized"}]}"""

        // WHEN / THEN
        assertThatThrownBy { createClient().createWorklog(TempoWorklogInput("a", 1L, "2026-09-14", "08:00:00", 60L)) }
            .isInstanceOf(TempoAccessDeniedException::class.java)
    }

    @Test
    fun shouldMapForbiddenToInvalidCredentials() {
        // GIVEN
        createResponse = 403 to """{"errors": [{"message": "Forbidden"}]}"""

        // WHEN / THEN
        assertThatThrownBy { createClient().createWorklog(TempoWorklogInput("a", 1L, "2026-09-14", "08:00:00", 60L)) }
            .isInstanceOf(TempoInvalidCredentialsException::class.java)
    }

    @Test
    fun shouldMapNotFoundToNotFound() {
        // GIVEN
        listResponse = 404 to """{"errors": [{"message": "User not found"}]}"""

        // WHEN / THEN
        assertThatThrownBy { createClient().getWorklogsForUser("unknown", LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 14)) }
            .isInstanceOf(TempoNotFoundException::class.java)
    }

    @Test
    fun shouldMapServerErrorToGenericClientException() {
        // GIVEN
        createResponse = 500 to "boom"

        // WHEN / THEN
        assertThatThrownBy { createClient().createWorklog(TempoWorklogInput("a", 1L, "2026-09-14", "08:00:00", 60L)) }
            .isInstanceOf(TempoClientException::class.java)
            .isNotInstanceOf(TempoAccessDeniedException::class.java)
            .hasMessageContaining("500")
    }

    @Test
    fun shouldRejectBlankBearerToken() {
        // WHEN / THEN
        assertThatThrownBy { TempoApiClient(baseUrl, "  ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
