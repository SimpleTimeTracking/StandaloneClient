package org.stt.event.events;

import org.stt.model.TimeTrackingItem;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 18.03.15.
 */
public class ItemDeletedEvent implements ItemModificationEvent {
    public final TimeTrackingItem deletedItem;

    public ItemDeletedEvent(TimeTrackingItem deletedItem) {
        this.deletedItem = checkNotNull(deletedItem);
    }
}
