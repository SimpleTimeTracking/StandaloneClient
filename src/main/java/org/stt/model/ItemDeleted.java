package org.stt.model;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 18.03.15.
 */
public class ItemDeleted implements ItemModified {
    public final TimeTrackingItem deletedItem;

    public ItemDeleted(TimeTrackingItem deletedItem) {
        this.deletedItem = checkNotNull(deletedItem);
    }
}
