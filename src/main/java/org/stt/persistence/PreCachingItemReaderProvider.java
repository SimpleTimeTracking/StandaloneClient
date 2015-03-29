package org.stt.persistence;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.stt.event.events.ItemDeletedEvent;
import org.stt.event.events.ItemInsertedEvent;
import org.stt.event.events.ItemReplacedEvent;
import org.stt.model.TimeTrackingItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Created by dante on 16.03.15.
 */
@Singleton
public class PreCachingItemReaderProvider implements ItemReaderProvider {
    private Object lock = new Object();
    private Collection<Optional<TimeTrackingItem>> cachedItems = new ArrayList<>();
    private ItemReaderProvider itemReaderProvider;

    @Inject
    public PreCachingItemReaderProvider(@Named("uncached") ItemReaderProvider itemReaderProvider) {
        this.itemReaderProvider = checkNotNull(itemReaderProvider);
    }

    @Subscribe
    public void sourceChanged(ItemDeletedEvent event) {
        rereadSource();
    }

    @Subscribe
    public void sourceChanged(ItemReplacedEvent event) {
        rereadSource();
    }

    @Subscribe
    public void sourceChanged(ItemInsertedEvent event) {
        rereadSource();
    }

    private void rereadSource() {
        synchronized (lock) {
            try (ItemReader reader = itemReaderProvider.provideReader()) {
                cachedItems = new ArrayList<>();
                Optional<TimeTrackingItem> read;
                do {
                    read = reader.read();
                    cachedItems.add(read);
                }
                while (read.isPresent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ItemReader provideReader() {
        synchronized (lock) {
            if (cachedItems.isEmpty()) {
                rereadSource();
            }
        }
        return new ItemReader() {
            Iterator<Optional<TimeTrackingItem>> itemIterator = cachedItems.iterator();

            @Override
            public Optional<TimeTrackingItem> read() {
                checkState(itemIterator != null, "ItemReader already closed!");
                return itemIterator.next();
            }

            @Override
            public void close() throws IOException {
                itemIterator = null;
            }
        };
    }
}
