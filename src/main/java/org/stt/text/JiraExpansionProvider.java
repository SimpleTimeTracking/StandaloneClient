package org.stt.text;

import net.engio.mbassy.bus.MBassador;
import net.rcarz.jiraclient.Issue;
import org.stt.connector.jira.*;
import org.stt.event.NotifyUser;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class JiraExpansionProvider implements ExpansionProvider {

    private static final Logger LOG = Logger.getLogger(JiraExpansionProvider.class
            .getName());

    private final JiraConnector jiraConnector;
    private final Optional<MBassador<Object>> eventBus;

    @Inject
    public JiraExpansionProvider(JiraConnector connector,
                                 Optional<MBassador<Object>> eventBus) {
        this.jiraConnector = requireNonNull(connector);
        this.eventBus = eventBus;
    }

    @Override
    public List<String> getPossibleExpansions(String text) {
        String queryText = text.trim();
        int spaceIndex = queryText.lastIndexOf(' ');
        if (spaceIndex > 0) {
            queryText = text.substring(spaceIndex, text.length()).trim();
        }

        try {
            return jiraConnector.getIssue(queryText)
                    .map(Issue::getSummary)
                    .map(issue -> Collections.singletonList(": " + issue))
                    .orElse(Collections.emptyList());
        } catch (JiraConnectorException | AccessDeniedException | InvalidCredentialsException | IssueDoesNotExistException e) {
            eventBus.ifPresent(eb -> eb.publish(new NotifyUser(e.getMessage())));
            return Collections.emptyList();
        }
    }

}
