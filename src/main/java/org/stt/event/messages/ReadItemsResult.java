package org.stt.event.messages;

import com.google.common.base.MoreObjects;
import org.stt.model.TimeTrackingItem;

import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

/**
 * Created by dante on 03.12.14.
 */
public class ReadItemsResult {
    public final Collection<TimeTrackingItem> timeTrackingItems;

    public ReadItemsResult(Collection<TimeTrackingItem> timeTrackingItems) {
        this.timeTrackingItems = Collections.unmodifiableCollection(checkNotNull(timeTrackingItems));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("items", timeTrackingItems.size()).toString();
    }
}
