package org.stt.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import java.util.concurrent.Executors;

/**
 * Created by dante on 03.12.14.
 */
public class EventBusModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(EventBus.class).toInstance(new AsyncEventBus(Executors.newCachedThreadPool(), new SubscriberExceptionHandler() {

            @Override
            public void handleException(Throwable exception, SubscriberExceptionContext context) {
                exception.printStackTrace();
            }
        }));
    }
}
