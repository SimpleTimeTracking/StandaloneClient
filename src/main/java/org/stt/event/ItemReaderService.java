package org.stt.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.stt.event.messages.ReadItemsEvent;
import org.stt.event.messages.ReadItemsRequest;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;

import java.io.IOException;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 03.12.14.
 */
public class ItemReaderService {
    private Provider<ItemReader> itemReaderProvider;
    private EventBus eventBus;

    @Inject
    public ItemReaderService(EventBus eventBus, Provider<ItemReader> itemReaderProvider) {
        this.eventBus = checkNotNull(eventBus);
        this.itemReaderProvider = checkNotNull(itemReaderProvider);
        eventBus.register(this);
    }

    @Subscribe
    public void acceptRequest(ReadItemsRequest request)  {
        ItemReader itemReader = itemReaderProvider.get();
        try {
            Collection<TimeTrackingItem> timeTrackingItems = IOUtil.readAll(itemReader);
            eventBus.post(new ReadItemsEvent(ReadItemsEvent.Type.START, timeTrackingItems));
            eventBus.post(new ReadItemsEvent(ReadItemsEvent.Type.DONE));
        } catch (IOException e) {
            eventBus.post(e);
        }
    }
}
