package org.stt.submit.tempo

/**
 * Base exception for all errors reported by the Tempo Cloud API client.
 *
 * Callers receive either a fully parsed result or one of these exceptions; no partial state is
 * exposed.
 */
open class TempoClientException(message: String) : RuntimeException(message)

class TempoAccessDeniedException(message: String) : TempoClientException(message)

class TempoInvalidCredentialsException(message: String) : TempoClientException(message)

class TempoNotFoundException(message: String) : TempoClientException(message)
