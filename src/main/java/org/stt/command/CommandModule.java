package org.stt.command;

import dagger.Module;
import dagger.Provides;
import net.engio.mbassy.bus.MBassador;
import org.stt.persistence.ItemPersister;
import org.stt.query.TimeTrackingItemQueries;

import java.util.Optional;

@Module
public class CommandModule {
    private CommandModule() {
    }

    @Provides
    public static CommandHandler provideCommandHandler(ItemPersister persister, TimeTrackingItemQueries queries, Optional<MBassador<Object>> eventBus) {
        return new Activities(persister, queries, eventBus);
    }
}
