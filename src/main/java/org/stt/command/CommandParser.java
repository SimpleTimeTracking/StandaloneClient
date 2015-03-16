package org.stt.command;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.g4.EnglishCommandsLexer;
import org.stt.g4.EnglishCommandsParser;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.search.ItemSearcher;
import org.stt.time.DateTimeHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 15.03.15.
 */
public class CommandParser {
    static final DateTimeFormatter FORMAT_HOUR_MINUTES_SECONDS = DateTimeFormat
            .forPattern("HH:mm:ss");

    static final DateTimeFormatter FORMAT_YEAR_MONTH_HOUR_MINUTES_SECONDS = DateTimeFormat
            .forPattern("yyyy.MM.dd HH:mm:ss");

    private final ItemPersister persister;
    private ItemGenerator itemGenerator = new ItemGenerator();
    private ItemSearcher itemSearcher;

    @Inject
    public CommandParser(ItemPersister persister, ItemSearcher itemSearcher) {
        this.persister = checkNotNull(persister);
        this.itemSearcher = checkNotNull(itemSearcher);
    }

    public Optional<Command> parseCommandString(String command) {
        checkNotNull(command);
        CharStream inputStream;
        inputStream = new CaseInsensitiveInputStream(command);
        EnglishCommandsLexer lexer = new EnglishCommandsLexer(inputStream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        EnglishCommandsParser parser = new EnglishCommandsParser(tokenStream);
        Object result = itemGenerator.walk(tokenStream, parser.command());
        if (result instanceof TimeTrackingItem) {
            TimeTrackingItem parsedItem = (TimeTrackingItem) result;
            return Optional.<Command>of(new NewItemCommand(persister, parsedItem));
        }
        if (result instanceof DateTime) {
            return endCurrentItemCommand((DateTime) result);
        }
        return Optional.absent();
    }

    public Optional<Command> endCurrentItemCommand(DateTime endTime) {
        Optional<TimeTrackingItem> currentTimeTrackingitem = itemSearcher
                .getCurrentTimeTrackingitem();
        if (currentTimeTrackingitem.isPresent()) {
            TimeTrackingItem unfinisheditem = currentTimeTrackingitem.get();
            TimeTrackingItem nowFinishedItem = unfinisheditem.withEnd(endTime);
            return Optional.<Command> of(new EndCurrentItemCommand(persister, unfinisheditem, nowFinishedItem));
        }
        return Optional.absent();
    }

    public Command resumeItemCommand(final TimeTrackingItem item) {
        return new ResumeCommand(persister, item);
    }

    public Command deleteCommandFor(TimeTrackingItem item) {
        return new DeleteCommand(persister, item);
    }

    public String itemToCommand(TimeTrackingItem item) {
        checkNotNull(item);
        DateTimeFormatter formatForStart = getShortFormatForTodayAndLongOtherwise(item
                .getStart());

        StringBuilder builder = new StringBuilder(item.getComment().or(""));
        builder.append(' ');
        if (item.getEnd().isPresent()) {
            builder.append("from ");
            builder.append(formatForStart.print(item.getStart()));
            builder.append(" to ");
            DateTimeFormatter formatForEnd = getShortFormatForTodayAndLongOtherwise(item
                    .getEnd().get());
            builder.append(formatForEnd.print(item.getEnd().get()));
        } else {
            builder.append("since ");
            builder.append(formatForStart.print(item.getStart()));
        }
        return builder.toString();
    }

    /**
     * @return short time format for today and long format otherwise
     */
    private DateTimeFormatter getShortFormatForTodayAndLongOtherwise(
            DateTime dateTime) {
        DateTimeFormatter formatForStart = FORMAT_YEAR_MONTH_HOUR_MINUTES_SECONDS;
        if (DateTimeHelper.isToday(dateTime)) {
            formatForStart = FORMAT_HOUR_MINUTES_SECONDS;
        }
        return formatForStart;
    }


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
