package org.stt.query;

import com.google.common.base.Preconditions;
import org.stt.model.TimeTrackingItem;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 05.04.15.
 */
public class QueryMatcher {
    private Query query;

    public QueryMatcher(Query query) {

        this.query = checkNotNull(query);
    }

    public boolean matches(TimeTrackingItem item) {
        checkNotNull(item);
        if (query.startBefore.isPresent() && !item.getStart().isBefore(query.startBefore.get())) {
            return false;
        }
        if (query.startNotBefore.isPresent() && item.getStart().isBefore(query.startNotBefore.get())) {
            return false;
        }
        if (query.endNotAfter.isPresent() && (!item.getEnd().isPresent() || item.getEnd().get().isAfter(query.endNotAfter.get()))) {
            return false;
        }
        if (query.endBefore.isPresent() && (!item.getEnd().isPresent() || !item.getEnd().get().isBefore(query.endBefore.get()))) {
            return false;
        }
        return true;
    }
}
