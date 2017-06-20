package org.stt.command;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.misc.Interval;

/**
 * Use case insensitive lookaheads but leave case of tokens.
 */
public class CaseInsensitiveInputStream implements CharStream {

    private final CodePointCharStream delegate;

    public CaseInsensitiveInputStream(String input) {
        delegate = CharStreams.fromString(input);
    }

    @Override
    public void consume() {
        delegate.consume();
    }

    @Override
    public int LA(int i) {
        int la = delegate.LA(i);
        if (Character.isAlphabetic(la)) {
            return Character.toLowerCase(la);
        }
        return la;
    }

    @Override
    public int mark() {
        return delegate.mark();
    }

    @Override
    public void release(int marker) {
        delegate.release(marker);
    }

    @Override
    public int index() {
        return delegate.index();
    }

    @Override
    public void seek(int index) {
        delegate.seek(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public String getSourceName() {
        return delegate.getSourceName();
    }

    @Override
    public String getText(Interval interval) {
        return delegate.getText(interval);
    }
}
