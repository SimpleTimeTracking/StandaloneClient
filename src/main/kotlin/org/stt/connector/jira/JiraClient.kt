package org.stt.connector.jira

import com.jsoniter.JsonIterator
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

class JiraClient(private var username: String?, private var password: String?, private var jiraUrl: String) {
    private val restApiV3 = "rest/api/3"
    private var restClient: HttpClient? = null

    init {
        this.restClient = getJiraRestClient()
    }

    private fun getJiraRestClient(): HttpClient? {
        return HttpClient.newBuilder().build()
    }


  fun getIssue(issueKey: String): Issue? {
    val jiraUrl = getJiraUrl()
    val request = requestBuilder("$jiraUrl$restApiV3/issue/$issueKey?fields=summary")
            .GET()
            .build()
    this.restClient?.send(request, HttpResponse.BodyHandlers.ofString())?.let {
      if (it.statusCode() == 200) {
        val deserializedAsMap = JsonIterator.deserialize(it.body(), Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val fields = deserializedAsMap["fields"] as Map<String, String>
        val id = (deserializedAsMap["id"] as String?)?.toLongOrNull()
        return Issue(fields["summary"], id)
      }
      handleHttpError(it, issueKey)
    }
    return null
  }

  fun getMyself(): Myself? {
    val jiraUrl = getJiraUrl()
    val request = requestBuilder("$jiraUrl$restApiV3/myself")
            .GET()
            .build()
    this.restClient?.send(request, HttpResponse.BodyHandlers.ofString())?.let {
      if (it.statusCode() == 200) {
        val deserializedAsMap = JsonIterator.deserialize(it.body(), Map::class.java)
        return Myself(deserializedAsMap["accountId"] as String?)
      }
      handleHttpError(it, "myself")
    }
    return null
  }

  private fun requestBuilder(url: String): HttpRequest.Builder {
    return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(("$username:$password").toByteArray()))
  }

  private fun handleHttpError(response: HttpResponse<String>, issueKey: String) {
    val httpStatusCode = response.statusCode()
    val body = response.body()
    when (httpStatusCode) {
      401 -> throw AccessDeniedException("You don't have permission to see $issueKey.")
      403 -> throw InvalidCredentialsException("Please check your Jira username/password.")
      404 -> throw IssueDoesNotExistException("Couldn't find issue $issueKey. Cause: $body")
      in 400..500 -> throw Exception("Error while retrieving issue $issueKey: $httpStatusCode: $body")
    }
  }

  private fun getJiraUrl(): String {
        val jiraUrlAsCharArray = this.jiraUrl.toCharArray()
        if(jiraUrlAsCharArray[jiraUrlAsCharArray.size - 1]  == '/') {
            return this.jiraUrl
        }
        return this.jiraUrl + "/"
    }

}

/**
 * The authenticated Jira user, as returned by the `myself` endpoint.
 */
class Myself(val accountId: String?)
