package org.stt.command;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.stt.grammar.EnglishCommandsLexer;
import org.stt.grammar.EnglishCommandsParser;
import org.stt.model.TimeTrackingItem;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class CommandFormatter {
    private final CommandTextParser commandTextParser;
    private final DateTimeFormatter dateTimeFormatter;
    private final DateTimeFormatter timeFormatter;

    @Inject
    public CommandFormatter(CommandTextParser commandTextParser,
                            @Named("dateTimeFormatter") DateTimeFormatter dateTimeFormatter,
                            @Named("timeFormatter") DateTimeFormatter timeFormatter) {
        this.commandTextParser = commandTextParser;
        this.dateTimeFormatter = dateTimeFormatter;
        this.timeFormatter = timeFormatter;
    }

    public Command parse(String command) {
        Objects.requireNonNull(command);
        CharStream inputStream = new CaseInsensitiveInputStream(command);
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
        String start;
        if (DateTimes.isToday(item.getStart())) {
            start = timeFormatter.format(item.getStart().toLocalTime());
        } else {
            start = dateTimeFormatter.format(item.getStart());
        }
        return item.getEnd()
                .map(endDateTime -> {
                            String end;
                            if (DateTimes.isToday(endDateTime)) {
                                end = timeFormatter.format(endDateTime.toLocalTime());
                            } else {
                                end = dateTimeFormatter.format(endDateTime);
                            }
                            return String.format("%s from %s to %s", item.getActivity(), start, end);
                        }
                )
                .orElseGet(() -> String.format("%s since %s", item.getActivity(), start));
    }

}
