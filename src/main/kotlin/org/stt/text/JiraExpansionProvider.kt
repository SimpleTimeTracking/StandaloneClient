package org.stt.text

import net.engio.mbassy.bus.MBassador
import org.stt.connector.jira.*
import org.stt.event.NotifyUser
import java.util.*
import javax.inject.Inject

class JiraExpansionProvider @Inject
constructor(private val jiraConnector: JiraConnector,
            private val eventBus: Optional<MBassador<Any>>) : ExpansionProvider {

    override fun getPossibleExpansions(text: String): List<String> {
        var queryText = text.trim { it <= ' ' }
        val spaceIndex = queryText.lastIndexOf(' ')
        if (spaceIndex > 0) {
            queryText = text.substring(spaceIndex, text.length).trim { it <= ' ' }
        }

        return try {
            jiraConnector.getIssue(queryText)?.summary?.let { listOf(": $it") } ?: emptyList()
        } catch (e: JiraConnectorException) {
            eventBus.ifPresent { eb -> eb.publish(NotifyUser(e.message ?: "")) }
            emptyList()
        } catch (e: Exceptions) {
            eventBus.ifPresent { eb -> eb.publish(NotifyUser(e.message ?: "")) }
            emptyList()
        } catch (e: InvalidCredentialsException) {
            eventBus.ifPresent { eb -> eb.publish(NotifyUser(e.message ?: "")) }
            emptyList()
        } catch (e: IssueDoesNotExistException) {
            eventBus.ifPresent { eb -> eb.publish(NotifyUser(e.message ?: "")) }
            emptyList()
        }

    }
}
