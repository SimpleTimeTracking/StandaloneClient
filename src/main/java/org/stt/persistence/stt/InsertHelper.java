package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemWriter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

class InsertHelper {
    private final ItemReader reader;
    private final ItemWriter writer;
    private final TimeTrackingItem itemToInsert;

    InsertHelper(ItemReader reader, ItemWriter writer,
                 TimeTrackingItem itemToInsert) {
        this.itemToInsert = Objects.requireNonNull(itemToInsert);
        this.writer = Objects.requireNonNull(writer);
        this.reader = Objects.requireNonNull(reader);
    }

    void performInsert() {
        Optional<TimeTrackingItem> lastReadItem = copyAllNonIntersectingItemsBeforeItemToInsert();
        adjustEndOfLastItemReadAndWrite(lastReadItem);
        writer.write(itemToInsert);
        lastReadItem = skipAllItemsCompletelyCoveredByItemToInsert(lastReadItem);
        adjustStartOfLastItemReadAndWrite(lastReadItem);
        copyRemainingItems();
    }

    private void adjustStartOfLastItemReadAndWrite(
            Optional<TimeTrackingItem> lastReadItem) {
        if (!lastReadItem.isPresent()) {
            return;
        }
        TimeTrackingItem item = lastReadItem.get();
        if (endOfReadItemIsAfterEndOfItemToInsert(item)) {
            if (startOfReadItemIsBeforeEndOfItemToInsert(item)) {
                TimeTrackingItem itemAfterItemToInsert = item
                        .withStart(itemToInsert.getEnd().get());
                writer.write(itemAfterItemToInsert);
            } else {
                writer.write(item);
            }
        }
    }

    private void adjustEndOfLastItemReadAndWrite(
            Optional<TimeTrackingItem> lastReadItem) {
        if (lastReadItem.isPresent()
                && startOfReadItemIsBeforeStartOfItemToInsert(lastReadItem
                .get())) {
            TimeTrackingItem itemBeforeItemToInsert = lastReadItem.get()
                    .withEnd(itemToInsert.getStart());
            writer.write(itemBeforeItemToInsert);
        }
    }

    private void copyRemainingItems() {
        Optional<TimeTrackingItem> lastReadItem;
        while ((lastReadItem = reader.read()).isPresent()) {
            writer.write(lastReadItem.get());
        }
    }

    private Optional<TimeTrackingItem> skipAllItemsCompletelyCoveredByItemToInsert(
            Optional<TimeTrackingItem> lastReadItem) {
        Optional<TimeTrackingItem> currentItem = lastReadItem;
        while (currentItem.isPresent()
                && !endOfReadItemIsAfterEndOfItemToInsert(currentItem.get())) {
            currentItem = reader.read();
        }
        return currentItem;
    }

    private Optional<TimeTrackingItem> copyAllNonIntersectingItemsBeforeItemToInsert() {
        Optional<TimeTrackingItem> lastReadItem;
        while ((lastReadItem = reader.read()).isPresent()
                && endOfReadItemIsBeforeOrEqualToStartOfItemToInsert(lastReadItem.get())) {
            writer.write(lastReadItem.get());
        }
        return lastReadItem;
    }

    private boolean startOfReadItemIsBeforeEndOfItemToInsert(TimeTrackingItem item) {
        return item.getStart().isBefore(itemToInsert.getEnd().get());
    }


    private boolean startOfReadItemIsBeforeStartOfItemToInsert(TimeTrackingItem lastReadItem) {
        return lastReadItem.getStart().isBefore(itemToInsert.getStart());
    }

    private boolean endOfReadItemIsAfterEndOfItemToInsert(
            TimeTrackingItem lastReadItem) {
        Optional<LocalDateTime> endOfLastReadItem = lastReadItem.getEnd();
        Optional<LocalDateTime> endOfItemToInsert = itemToInsert.getEnd();
        return endOfItemToInsert.isPresent() && (!endOfLastReadItem.isPresent() || endOfLastReadItem.get().isAfter(endOfItemToInsert.get()));
    }

    private boolean endOfReadItemIsBeforeOrEqualToStartOfItemToInsert(
            TimeTrackingItem lastReadItem) {
        Optional<LocalDateTime> endOfLastReadItem = lastReadItem.getEnd();
        return endOfLastReadItem.isPresent()
                && !endOfLastReadItem.get().isAfter(itemToInsert.getStart());
    }
}
