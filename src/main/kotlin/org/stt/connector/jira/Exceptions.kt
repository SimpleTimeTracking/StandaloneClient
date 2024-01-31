package org.stt.connector.jira

class Exceptions(message: String, cause: Throwable) : Exception(message, cause)
class InvalidCredentialsException(message: String) : Exception(message)
class IssueDoesNotExistException(message: String) : Exception(message)
class AccessDeniedException(message: String) : Exception(message)
class JiraConnectorException(errorMessage: String, cause: Exception) : RuntimeException(errorMessage, cause)

