package org.stt.command

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CodePointCharStream
import org.antlr.v4.runtime.misc.Interval

/**
 * Use case insensitive lookaheads but leave case of tokens.
 */
class CaseInsensitiveInputStream(input: String) : CharStream {

    private val delegate: CodePointCharStream = CharStreams.fromString(input)

    override fun consume() {
        delegate.consume()
    }

    override fun LA(i: Int): Int {
        val la = delegate.LA(i)
        return if (Character.isAlphabetic(la)) {
            Character.toLowerCase(la)
        } else la
    }

    override fun mark(): Int {
        return delegate.mark()
    }

    override fun release(marker: Int) {
        delegate.release(marker)
    }

    override fun index(): Int {
        return delegate.index()
    }

    override fun seek(index: Int) {
        delegate.seek(index)
    }

    override fun size(): Int {
        return delegate.size()
    }

    override fun getSourceName(): String {
        return delegate.sourceName
    }

    override fun getText(interval: Interval): String {
        return delegate.getText(interval)
    }
}
