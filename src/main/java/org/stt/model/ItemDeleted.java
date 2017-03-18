package org.stt.model;

import java.util.Objects;

public class ItemDeleted implements ItemModified {
    public final TimeTrackingItem deletedItem;

    public ItemDeleted(TimeTrackingItem deletedItem) {
        this.deletedItem = Objects.requireNonNull(deletedItem);
    }
}
