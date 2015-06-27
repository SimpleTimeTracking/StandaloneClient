package org.stt.model;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 18.03.15.
 */
public class ItemReplaced implements ItemModified {
    public final TimeTrackingItem beforeUpdate;
    public final TimeTrackingItem afterUpdate;

    public ItemReplaced(TimeTrackingItem beforeUpdate, TimeTrackingItem afterUpdate) {
        this.beforeUpdate = checkNotNull(beforeUpdate);
        this.afterUpdate = checkNotNull(afterUpdate);
    }
}
