package org.stt.event

import dagger.Module
import dagger.Provides
import net.engio.mbassy.bus.MBassador
import java.util.logging.Logger
import javax.inject.Singleton

@Module
class EventBusModule {
    private val log = Logger.getLogger(EventBusModule::class.java.simpleName)

    @Provides
    @Singleton
    fun provideMBassador(): MBassador<Any> {
        return MBassador { error -> log.severe { error.toString() } }
    }
}
