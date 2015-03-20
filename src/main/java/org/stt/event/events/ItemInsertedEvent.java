package org.stt.event.events;

import org.stt.model.TimeTrackingItem;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 18.03.15.
 */
public class ItemInsertedEvent {
    public final TimeTrackingItem newItem;

    public ItemInsertedEvent(TimeTrackingItem newItem) {
        this.newItem = checkNotNull(newItem);
    }
}
