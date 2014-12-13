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
public class ReadItemsEvent {
    public final Type type;
    public final Collection<TimeTrackingItem> timeTrackingItems;

    public ReadItemsEvent(Type type, Collection<TimeTrackingItem> timeTrackingItems) {
        this.type = checkNotNull(type);
        this.timeTrackingItems = Collections.unmodifiableCollection(checkNotNull(timeTrackingItems));
    }

    public ReadItemsEvent(Type type) {
        this.type = type;
        timeTrackingItems = emptyList();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("type", type).add("items", timeTrackingItems.size()).toString();
    }

    public enum Type {
        START, CONTINUE, DONE
    }
}
