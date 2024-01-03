package org.stt.connector.jira

import org.stt.Service
import org.stt.config.JiraConfig
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class JiraConnector @Inject
constructor(configuration: JiraConfig) : Service {
    private val client: JiraClient?

    init {
        val jiraURI = configuration.jiraURI
        client = jiraURI?.let {
            if (configuration.jiraUsername != null
                    && configuration.jiraUsername!!.isNotEmpty()
                    && configuration.jiraToken != null) {
                JiraClient(configuration.jiraUsername!!,
                                String(configuration.jiraToken!!.password, StandardCharsets.UTF_8), jiraURI)
            } else {
                throw InvalidCredentialsException("Credentials missing", Exception("No username or token configured for Jira connector."))
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

        try {
            return client.getIssue(issueKey)
        } catch (e: Exception) {
            throw JiraConnectorException(String.format("Error while retrieving issue %s: %s", issueKey, e.cause?.localizedMessage), e)
        }

    }

}
