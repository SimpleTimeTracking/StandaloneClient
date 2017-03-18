package org.stt.connector.jira;

public class JiraConnectorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7977072219051943054L;

	public JiraConnectorException(String errorMessage, Exception cause) {
		super(errorMessage, cause);
	}

}
