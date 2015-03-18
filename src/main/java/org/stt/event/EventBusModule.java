package org.stt.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.inject.AbstractModule;
import org.stt.persistence.ItemPersister;

import java.util.concurrent.Executors;

/**
 * Created by dante on 03.12.14.
 */
public class EventBusModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(EventBus.class).toInstance(new EventBus(new SubscriberExceptionHandler() {

            @Override
            public void handleException(Throwable exception, SubscriberExceptionContext context) {
                exception.printStackTrace();
            }
        }));
        bind(ItemPersister.class).annotatedWith(EventBusAware.class).to(EventDispatchingItemPersister.class);
    }
}
