package org.stt.query;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.stt.model.ItemModified;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.stt.Streams.distinctByKey;

@Singleton
public class TimeTrackingItemQueries {
    private static final Logger LOG = Logger.getLogger(TimeTrackingItemQueries.class.getSimpleName());
    private final Provider<ItemReader> provider;
    private List<TimeTrackingItem> cachedItems;

	/**
     * @param provider
     *            where to search for items
     */
    @Inject
    public TimeTrackingItemQueries(Provider<ItemReader> provider,
                                   Optional<MBassador<Object>> eventbus) {
        this.provider = Objects.requireNonNull(provider);
        eventbus.ifPresent(bus -> bus.subscribe(this));
    }

    @Handler
    public synchronized void sourceChanged(ItemModified event) {
        cachedItems = null;
        LOG.fine("Clearing query cache");
    }

    public Optional<TimeTrackingItem> getCurrentTimeTrackingitem() {
        try (Stream<TimeTrackingItem> items = queryAllItems()) {
            return items
                    .reduce((a, b) -> b)
                    .filter(item -> !item.getEnd().isPresent());
        }
    }

    public Optional<TimeTrackingItem> getLastTimeTrackingItem() {
        validateCache();
        return cachedItems.isEmpty() ? Optional.empty() : Optional.of(cachedItems.get(cachedItems.size() - 1));
    }

    /**
     * @return a {@link Stream} containing all time tracking items, be sure to {@link Stream#close()} it!
     */
    public Stream<LocalDate> queryAllTrackedDays() {
        return queryAllItems()
                .map(TimeTrackingItem::getStart)
                .map(LocalDateTime::toLocalDate)
                .distinct();
    }

    public Stream<TimeTrackingItem> queryFirstItemsOfDays() {
        return queryAllItems()
                .filter(distinctByKey(item -> item.getStart().toLocalDate()));
    }

    /**
     * @return a {@link Stream} containing all time tracking items matching the given criteria, be sure to {@link Stream#close()} it!
     */
    public Stream<TimeTrackingItem> queryItems(Criteria criteria) {
        return queryAllItems()
                .filter(criteria::matches);
    }

    /**
     * @return a {@link Stream} containing all time tracking items, be sure to {@link Stream#close()} it!
     */
    public synchronized Stream<TimeTrackingItem> queryAllItems() {
        validateCache();
        return cachedItems.stream();
    }

    private void validateCache() {
        if (cachedItems == null) {
            LOG.fine("Rebuilding cache");
            cachedItems = new ArrayList<>();
            try (TimeTrackingItemIterator it = new TimeTrackingItemIterator(provider.get())) {
                while (it.hasNext()) {
                    cachedItems.add(it.next());
                }
            }
        }
    }

    private static class TimeTrackingItemIterator implements Iterator<TimeTrackingItem>, AutoCloseable {
        private final ItemReader sourceReader;
        private TimeTrackingItem nextItem;

        TimeTrackingItemIterator(ItemReader sourceReader) {
            this.sourceReader = sourceReader;
        }

        @Override
        public boolean hasNext() {
            if (nextItem != null) {
                return true;
            }
            nextItem = sourceReader.read().orElse(null);
            return nextItem != null;
        }

        @Override
        public TimeTrackingItem next() {
            if (nextItem != null || hasNext()) {
                TimeTrackingItem itemToReturn = nextItem;
                nextItem = null;
                return itemToReturn;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void close() {
            sourceReader.close();
        }
    }
}
