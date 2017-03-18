package org.stt.model;

import java.util.Objects;

public class ItemInserted implements ItemModified {
    public final TimeTrackingItem newItem;

    public ItemInserted(TimeTrackingItem newItem) {
        this.newItem = Objects.requireNonNull(newItem);
    }
}
