package org.stt.command;

import dagger.Module;
import dagger.Provides;
import net.engio.mbassy.bus.MBassador;
import org.stt.persistence.ItemPersister;
import org.stt.query.TimeTrackingItemQueries;

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
                new DateTimeFormatterBuilder().parseLenient().appendLocalized(null, FormatStyle.SHORT).toFormatter(),
                new DateTimeFormatterBuilder().parseLenient().appendLocalized(FormatStyle.SHORT, FormatStyle.SHORT).toFormatter());
    }
}
