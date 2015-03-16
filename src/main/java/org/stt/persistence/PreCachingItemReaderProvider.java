package org.stt.persistence;

import com.google.common.base.Optional;
import org.stt.model.TimeTrackingItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 16.03.15.
 */
public class PreCachingItemReaderProvider implements ItemReaderProvider {
    private Collection<Optional<TimeTrackingItem>> cachedItems = new ArrayList<>();

    public PreCachingItemReaderProvider(ItemReader delegate) {
        Optional<TimeTrackingItem> read;
        do {
            read = delegate.read();
            cachedItems.add(read);
        }
        while (read.isPresent());
    }

    @Override
    public ItemReader provideReader() {
        return new ItemReader() {
            Iterator<Optional<TimeTrackingItem>> itemIterator = cachedItems.iterator();

            @Override
            public Optional<TimeTrackingItem> read() {
                return itemIterator.next();
            }

            @Override
            public void close() throws IOException {
                itemIterator = null;
            }
        };
    }
}
