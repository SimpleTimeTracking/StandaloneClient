package org.stt.connector.jira;

public class JiraConnectorException extends RuntimeException {
    public JiraConnectorException(String errorMessage, Exception cause) {
		super(errorMessage, cause);
	}
}
