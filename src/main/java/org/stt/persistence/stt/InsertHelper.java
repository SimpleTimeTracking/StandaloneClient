package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemWriter;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

class InsertHelper {
    private final ItemReader reader;
    private final ItemWriter writer;
    private final TimeTrackingItem itemToInsert;
    private Optional<TimeTrackingItem> lastReadItem;

    InsertHelper(ItemReader reader, ItemWriter writer,
                 TimeTrackingItem itemToInsert) {
        this.itemToInsert = Objects.requireNonNull(itemToInsert);
        this.writer = Objects.requireNonNull(writer);
        this.reader = Objects.requireNonNull(reader);
    }

    void performInsert() {
        copyAllItemsEndingAtOrBeforeItemToInsert();
        lastReadItem.ifPresent(this::adjustEndOfLastItemReadAndWrite);
        writer.write(itemToInsert);
        skipAllItemsCompletelyCoveredByItemToInsert();
        lastReadItem.ifPresent(this::adjustStartOfLastItemReadAndWrite);
        copyRemainingItems();
    }

    private void copyAllItemsEndingAtOrBeforeItemToInsert() {
        copyWhile(item -> item.endsAtOrBefore(itemToInsert.getStart()));
    }

    private void adjustStartOfLastItemReadAndWrite(TimeTrackingItem item) {
        TimeTrackingItem itemToWrite = itemToInsert.getEnd()
                .map(end -> end.isAfter(item.getStart()) ? item.withStart(end) : null)
                .orElse(item);
        writer.write(itemToWrite);
    }

    private void adjustEndOfLastItemReadAndWrite(TimeTrackingItem item) {
        if (item.getStart().isBefore(itemToInsert.getStart())) {
            TimeTrackingItem itemBeforeItemToInsert = item
                    .withEnd(itemToInsert.getStart());
            writer.write(itemBeforeItemToInsert);
        }
    }

    private void copyRemainingItems() {
        copyWhile(item -> true);
    }

    private void skipAllItemsCompletelyCoveredByItemToInsert() {
        Optional<TimeTrackingItem> currentItem = lastReadItem;
        while (currentItem.isPresent() && itemToInsert.endsSameOrAfter(currentItem.get())) {
            currentItem = reader.read();
        }
        lastReadItem = currentItem;
    }

    private void copyWhile(Function<TimeTrackingItem, Boolean> condition) {
        while ((lastReadItem = reader.read()).isPresent()
                && condition.apply(lastReadItem.get())) {
            writer.write(lastReadItem.get());
        }
    }
}
