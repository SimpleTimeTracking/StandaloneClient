package org.stt.command;

import dagger.Module;
import dagger.Provides;
import net.engio.mbassy.bus.MBassador;
import org.stt.persistence.ItemPersister;
import org.stt.query.TimeTrackingItemQueries;

import javax.inject.Named;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Optional;

@Module
public class CommandModule {
    private CommandModule() {
    }

    @Provides
    public static CommandHandler provideCommandHandler(ItemPersister persister, TimeTrackingItemQueries queries, Optional<MBassador<Object>> eventBus) {
        return new Activities(persister, queries, eventBus);
    }

    @Provides
    public static CommandTextParser provideCommandTextParser() {
        return new CommandTextParser(
                dateTimeFormatter(null, FormatStyle.SHORT),
                dateTimeFormatter(null, FormatStyle.MEDIUM),
                dateTimeFormatter(FormatStyle.SHORT, FormatStyle.SHORT),
                dateTimeFormatter(FormatStyle.SHORT, FormatStyle.MEDIUM),
                dateTimeFormatter(FormatStyle.MEDIUM, FormatStyle.SHORT),
                dateTimeFormatter(FormatStyle.MEDIUM, FormatStyle.MEDIUM));
    }

    protected static DateTimeFormatter dateTimeFormatter(FormatStyle dateFormat, FormatStyle timeFormat) {
        return new DateTimeFormatterBuilder()
                .parseLenient().appendLocalized(dateFormat, timeFormat).toFormatter();
    }

    @Provides
    @Named("dateTimeFormatter")
    public static DateTimeFormatter provideDateTimeFormatter() {
        return new DateTimeFormatterBuilder()
                .appendLocalized(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
                .toFormatter();
    }

    @Provides
    @Named("timeFormatter")
    public static DateTimeFormatter provideTimeFormatter() {
        return new DateTimeFormatterBuilder()
                .appendLocalized(null, FormatStyle.MEDIUM)
                .toFormatter();
    }
}
