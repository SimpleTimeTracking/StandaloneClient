package org.stt.submit.tempo

import com.jsoniter.JsonIterator
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.util.Base64
import java.util.logging.Logger

/**
 * A single Tempo worklog as returned by the Tempo Cloud API.
 */
class TempoWorklog(
    val tempoWorklogId: Long?,
    val issueId: Long?,
    val startDate: String?,
    val startTime: String?,
    val timeSpentSeconds: Long?
)

/**
 * The payload for creating a Tempo worklog.
 */
class TempoWorklogInput(
    val authorAccountId: String,
    val issueId: Long,
    val startDate: String,
    val startTime: String,
    val timeSpentSeconds: Long
)

/**
 * Client for the Tempo Cloud REST API v4, authenticated with a Tempo user token (Bearer).
 *
 * Creates worklogs and queries existing worklogs of a user for a date range, which is used to
 * prevent double submission on days that already have worklogs.
 */
class TempoApiClient(private val tempoApiBaseUrl: String, private val bearerToken: String) {

    private val restClient: HttpClient = HttpClient.newBuilder().build()

    init {
        require(bearerToken.isNotBlank()) { "Tempo bearer token must not be blank" }
    }

    /**
     * Creates a worklog and returns the created worklog as confirmed by Tempo.
     *
     * @throws TempoClientException if Tempo rejects the request or returns an unexpected response
     */
    fun createWorklog(input: TempoWorklogInput): TempoWorklog {
        val body = buildWorklogJson(input)
        val request = requestBuilder("$tempoApiBaseUrl/worklogs")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        val response = restClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw tempoException(response.statusCode(), "creating worklog on ${input.startDate} for issue ${input.issueId}", response.body())
        }
        val deserializedAsMap = JsonIterator.deserialize(response.body(), Map::class.java)
        return parseWorklog(deserializedAsMap.asStringMap())
    }

    /**
     * Returns all worklogs of the user with the given account id between [from] and [to] (inclusive).
     *
     * @throws TempoClientException if Tempo rejects the request or returns an unexpected response
     */
    fun getWorklogsForUser(accountId: String, from: LocalDate, to: LocalDate): List<TempoWorklog> {
        val url = "$tempoApiBaseUrl/worklogs/user/${accountId}?from=$from&to=$to&limit=1000"
        val request = requestBuilder(url)
                .GET()
                .build()
        val response = restClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw tempoException(response.statusCode(), "retrieving worklogs of user between $from and $to", response.body())
        }
        val deserializedAsMap = JsonIterator.deserialize(response.body(), Map::class.java)
        val results = (deserializedAsMap["results"] as List<*>? ?: emptyList<Any?>())
                .filterIsInstance<Map<*, *>>()
                .map { it.asStringMap() }
        return results.map { parseWorklog(it) }
    }

    private fun parseWorklog(worklogMap: Map<String, Any?>): TempoWorklog {
        @Suppress("UNCHECKED_CAST")
        val issue = worklogMap["issue"] as Map<String, Any?>?
        val issueId = (issue?.get("id") as? String)?.toLongOrNull()
                ?: (issue?.get("id") as? Long)
                ?: (issue?.get("id") as? Int)?.toLong()
        return TempoWorklog(
                (worklogMap["id"] as? String)?.toLongOrNull() ?: (worklogMap["id"] as? Long) ?: (worklogMap["id"] as? Int)?.toLong(),
                issueId,
                worklogMap["startDate"] as String?,
                worklogMap["startTime"] as String?,
                (worklogMap["timeSpentSeconds"] as? Int)?.toLong() ?: worklogMap["timeSpentSeconds"] as? Long
        )
    }

    private fun buildWorklogJson(input: TempoWorklogInput): String {
        return StringBuilder()
                .append("{")
                .append("\"authorAccountId\": \"").append(input.authorAccountId).append("\", ")
                .append("\"issueId\": ").append(input.issueId).append(", ")
                .append("\"startDate\": \"").append(input.startDate).append("\", ")
                .append("\"startTime\": \"").append(input.startTime).append("\", ")
                .append("\"timeSpentSeconds\": ").append(input.timeSpentSeconds)
                .append("}")
                .toString()
    }

    private fun requestBuilder(url: String): HttpRequest.Builder {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + Base64.getEncoder().encodeToString(bearerToken.toByteArray()))
    }

    private fun Map<*, *>.asStringMap(): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        for ((key, value) in this) {
            result[key.toString()] = value
        }
        return result
    }

    private fun tempoException(statusCode: Int, action: String, body: String): TempoClientException {
        return when (statusCode) {
            401 -> TempoAccessDeniedException("You don't have permission to $action.")
            403 -> TempoInvalidCredentialsException("Please check your Tempo token while trying to $action.")
            404 -> TempoNotFoundException("Couldn't find the resource while trying to $action. Cause: $body")
            else -> TempoClientException("Error while trying to $action: $statusCode: $body")
        }
    }

    companion object {
        const val TEMPO_CLOUD_BASE_URL = "https://api.tempo.io/4"
        private val LOG = Logger.getLogger(TempoApiClient::class.java.name)
    }
}
