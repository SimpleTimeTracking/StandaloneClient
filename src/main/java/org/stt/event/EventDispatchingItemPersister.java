package org.stt.event;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.stt.event.events.ItemDeletedEvent;
import org.stt.event.events.ItemInsertedEvent;
import org.stt.event.events.ItemReplacedEvent;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 18.03.15.
 */
@Singleton
public class EventDispatchingItemPersister implements ItemPersister {
    private ItemPersister delegate;
    private EventBus eventBus;

    @Inject
    public EventDispatchingItemPersister(ItemPersister delegate, EventBus eventBus) {
        this.eventBus = checkNotNull(eventBus);
        this.delegate = checkNotNull(delegate);
    }


    @Override
    public void insert(TimeTrackingItem item) throws IOException {
        delegate.insert(item);
        eventBus.post(new ItemInsertedEvent(item));
    }

    @Override
    public void replace(TimeTrackingItem item, TimeTrackingItem with) throws IOException {
        delegate.replace(item, with);
        eventBus.post(new ItemReplacedEvent(item, with));
    }

    @Override
    public void delete(TimeTrackingItem item) throws IOException {
        delegate.delete(item);
        eventBus.post(new ItemDeletedEvent(item));
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
