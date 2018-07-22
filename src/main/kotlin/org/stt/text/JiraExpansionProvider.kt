package org.stt.text

import net.engio.mbassy.bus.MBassador
import org.stt.connector.jira.*
import org.stt.event.NotifyUser
import java.util.*
import java.util.Objects.requireNonNull
import java.util.logging.Logger
import javax.inject.Inject

class JiraExpansionProvider @Inject
constructor(connector: JiraConnector,
            private val eventBus: Optional<MBassador<Any>>) : ExpansionProvider {

    private val jiraConnector: JiraConnector

    init {
        this.jiraConnector = requireNonNull(connector)
    }

    override fun getPossibleExpansions(text: String): List<String> {
        var queryText = text.trim { it <= ' ' }
        val spaceIndex = queryText.lastIndexOf(' ')
        if (spaceIndex > 0) {
            queryText = text.substring(spaceIndex, text.length).trim { it <= ' ' }
        }

        try {
            return jiraConnector.getIssue(queryText)
                    .map { it.getSummary() }
                    .map { issue -> listOf(": $issue") }
                    .orElse(emptyList())
        } catch (e: JiraConnectorException) {
            eventBus.ifPresent { eb -> eb.publish(NotifyUser(e.message ?: "")) }
            return emptyList()
        } catch (e: Exceptions) {
            eventBus.ifPresent { eb -> eb.publish(NotifyUser(e.message ?: "")) }
            return emptyList()
        } catch (e: InvalidCredentialsException) {
            eventBus.ifPresent { eb -> eb.publish(NotifyUser(e.message ?: "")) }
            return emptyList()
        } catch (e: IssueDoesNotExistException) {
            eventBus.ifPresent { eb -> eb.publish(NotifyUser(e.message ?: "")) }
            return emptyList()
        }

    }

    companion object {

        private val LOG = Logger.getLogger(JiraExpansionProvider::class.java
                .name)
    }

}
