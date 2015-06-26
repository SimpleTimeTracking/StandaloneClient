package org.stt.model;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 18.03.15.
 */
public class ItemInserted implements ItemModificationEvent {
    public final TimeTrackingItem newItem;

    public ItemInserted(TimeTrackingItem newItem) {
        this.newItem = checkNotNull(newItem);
    }
}
