package org.stt.command;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.stt.g4.EnglishCommandsLexer;
import org.stt.g4.EnglishCommandsParser;
import org.stt.model.TimeTrackingItem;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Objects;

public class CommandFormatter {
    private CommandTextParser commandTextParser = new CommandTextParser();

    @Inject
    public CommandFormatter() {
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
    private static class CaseInsensitiveInputStream extends ANTLRInputStream {
        protected CaseInsensitiveInputStream(String input) {
            super(input);
        }

        @Override
        public int LA(int i) {
            int la = super.LA(i);
            if (Character.isAlphabetic(la)) {
                return Character.toLowerCase(la);
            }
            return la;
        }
    }

}
