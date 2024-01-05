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


    fun getIssue(issueKey: String): Issue {
        val jiraUrl = getJiraUrl()
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$jiraUrl$restApiV3/issue/$issueKey?fields=summary"))
                .GET()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(("$username:$password").toByteArray()))
                .build()
        this.restClient?.send(request, HttpResponse.BodyHandlers.ofString())?.let {
            if(it.statusCode() == 200) {
                val deserializedAsMap = JsonIterator.deserialize(it.body(), Map::class.java)
                val fields = deserializedAsMap["fields"] as Map<String, String>
                return Issue(fields["summary"])
            }
        }
                return Issue()


    }

    private fun getJiraUrl(): String {
        val jiraUrlAsCharArray = this.jiraUrl.toCharArray()
        if(jiraUrlAsCharArray[jiraUrlAsCharArray.size - 1]  == '/') {
            return this.jiraUrl
        }
        return this.jiraUrl + "/"
    }

}
