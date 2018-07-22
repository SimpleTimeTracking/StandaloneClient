package org.stt.connector.jira

class Exceptions(message: String, cause: Throwable) : Exception(message, cause)
class InvalidCredentialsException(message: String, cause: Throwable) : Exception(message, cause)
class IssueDoesNotExistException(message: String, cause: Throwable) : Exception(message, cause)
class JiraConnectorException(errorMessage: String, cause: Exception) : RuntimeException(errorMessage, cause)

