package org.stt.model;

import java.util.Objects;

public class ItemReplaced implements ItemModified {
    public final TimeTrackingItem beforeUpdate;
    public final TimeTrackingItem afterUpdate;

    public ItemReplaced(TimeTrackingItem beforeUpdate, TimeTrackingItem afterUpdate) {
        this.beforeUpdate = Objects.requireNonNull(beforeUpdate);
        this.afterUpdate = Objects.requireNonNull(afterUpdate);
    }
}
