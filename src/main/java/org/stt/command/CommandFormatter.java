package org.stt.command;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.stt.grammar.EnglishCommandsLexer;
import org.stt.grammar.EnglishCommandsParser;
import org.stt.model.TimeTrackingItem;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Objects;

public class CommandFormatter {
    private CommandTextParser commandTextParser = new CommandTextParser();

    @Inject
    public CommandFormatter() {
        // Required by Dagger
    }

    public Command parse(String command) {
        Objects.requireNonNull(command);
        CharStream inputStream;
        inputStream = new CaseInsensitiveInputStream(command);
        EnglishCommandsLexer lexer = new EnglishCommandsLexer(inputStream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        EnglishCommandsParser parser = new EnglishCommandsParser(tokenStream);
        Object result = commandTextParser.walk(parser.command());
        if (result instanceof TimeTrackingItem) {
            TimeTrackingItem parsedItem = (TimeTrackingItem) result;
            return new NewActivity(parsedItem);
        }
        if (result instanceof LocalDateTime) {
            return new EndCurrentItem((LocalDateTime) result);
        }
        if (result instanceof Command) {
            return (Command) result;
        }
        return DoNothing.INSTANCE;
    }

    public String asNewItemCommandText(TimeTrackingItem item) {
        Objects.requireNonNull(item);
        String startFormatted = DateTimes.prettyPrintTime(item.getStart());
        return item.getEnd()
                .map(endDateTime ->
                        String.format("%s from %s to %s", item.getActivity(), startFormatted, DateTimes.prettyPrintTime(endDateTime)))
                .orElseGet(() -> String.format("%s since %s", item.getActivity(), startFormatted));
    }

    /**
     * Use case insensitive lookaheads but leave case of tokens.
     */
    private static class CaseInsensitiveInputStream implements CharStream {

        private final CodePointCharStream delegate;

        CaseInsensitiveInputStream(String input) {
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

}
