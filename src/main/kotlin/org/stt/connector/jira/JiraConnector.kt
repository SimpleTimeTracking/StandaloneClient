package org.stt.connector.jira

import net.rcarz.jiraclient.*
import org.stt.Service
import org.stt.config.JiraConfig
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class JiraConnector @Inject
constructor(configuration: JiraConfig) : Service {
    private val client: JiraClient?

    private val projectNames: Set<String> by lazy {
        internalGetProjectNames()
    }

    init {
        val jiraURI = configuration.jiraURI
        client = jiraURI?.let {
            if (configuration.jiraUsername != null
                    && configuration.jiraUsername!!.isNotEmpty()
                    && configuration.jiraPassword != null) {
                JiraClient(jiraURI,
                        BasicCredentials(configuration.jiraUsername,
                                String(configuration.jiraPassword!!.password, StandardCharsets.UTF_8)))
            } else {
                JiraClient(jiraURI)
            }
        }
    }


    override fun start() {
        // no further initialization needed
    }

    override fun stop() {
        // no cleanup
    }

    fun getIssue(issueKey: String): Issue? {
        if (client == null) {
            return null
        }

        val projectKey = getProjectKey(issueKey)

        // Check if the given project key belongs to an existing project
        if (!projectExists(projectKey)) {
            return null
        }

        try {
            return client.getIssue(issueKey)
        } catch (e: JiraException) {
            if (e.cause is RestException) {
                val cause = e.cause as RestException
                val httpStatusCode = cause.httpStatusCode
                if (404 == httpStatusCode) {
                    throw IssueDoesNotExistException(String.format("Couldn't find issue %s.", issueKey), e)
                } else if (401 == httpStatusCode) {
                    throw Exceptions(String.format("You don't have permission to see %s.", issueKey), e)
                }
            }
            throw JiraConnectorException(String.format("Error while retrieving issue %s: %s", issueKey, e.cause?.localizedMessage), e)
        }

    }

    private fun projectExists(projectKey: String): Boolean {
        return projectNames.contains(projectKey)
    }

    private fun getProjectKey(issueKey: String): String {
        val index = issueKey.lastIndexOf('-')

        // Extract the project key
        return if (index > 0) issueKey.substring(0, index) else issueKey
    }

    private fun internalGetProjectNames(): Set<String> {
        try {
            return client!!.projects
                    .map { it.key }
                    .toSet()
        } catch (e: JiraException) {
            if (e.cause is RestException) {
                val cause = e.cause as RestException
                val httpStatusCode = cause.httpStatusCode
                if (httpStatusCode == 403 || httpStatusCode == 401) {
                    throw InvalidCredentialsException("Please check your Jira username/password.", e)
                }
            }
            throw JiraConnectorException(String.format("Error retrieving projects from Jira: %s", e.localizedMessage), e)
        }

    }
}
