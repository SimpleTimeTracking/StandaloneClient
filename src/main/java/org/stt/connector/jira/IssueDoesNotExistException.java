package org.stt.connector.jira;

public class IssueDoesNotExistException extends Exception {
    public IssueDoesNotExistException(String message) {
        super(message);
    }

    public IssueDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public IssueDoesNotExistException(Throwable cause) {
        super(cause);
    }
}
