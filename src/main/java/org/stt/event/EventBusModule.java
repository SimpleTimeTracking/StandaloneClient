package org.stt.event;

import dagger.Module;
import dagger.Provides;
import net.engio.mbassy.bus.MBassador;

import javax.inject.Singleton;
import java.util.logging.Logger;

/**
 * Created by dante on 03.12.14.
 */
@Module
public abstract class EventBusModule {
    private static final Logger LOG = Logger.getLogger(EventBusModule.class.getSimpleName());

    private EventBusModule() {
    }

    @Provides
    @Singleton
    static MBassador<Object> provideMBassador() {
        return new MBassador<>(error -> LOG.severe(error::toString));
    }
}
