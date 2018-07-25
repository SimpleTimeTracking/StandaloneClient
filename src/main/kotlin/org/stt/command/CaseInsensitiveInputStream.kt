package org.stt.command

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams

open class CharStreamDelegate(protected val delegate: CharStream) : CharStream by delegate

/**
 * Use case insensitive lookaheads but leave case of tokens.
 */
class CaseInsensitiveInputStream(input: String) : CharStreamDelegate(CharStreams.fromString(input)) {
    override fun LA(i: Int): Int {
        val la = delegate.LA(i)
        return if (Character.isAlphabetic(la)) {
            Character.toLowerCase(la)
        } else la
    }
}
