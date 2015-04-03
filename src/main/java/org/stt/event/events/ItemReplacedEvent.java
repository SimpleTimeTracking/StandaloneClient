package org.stt.event.events;

import org.stt.model.TimeTrackingItem;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 18.03.15.
 */
public class ItemReplacedEvent implements ItemModificationEvent {
    public final TimeTrackingItem beforeUpdate;
    public final TimeTrackingItem afterUpdate;

    public ItemReplacedEvent(TimeTrackingItem beforeUpdate, TimeTrackingItem afterUpdate) {
        this.beforeUpdate = checkNotNull(beforeUpdate);
        this.afterUpdate = checkNotNull(afterUpdate);
    }
}
