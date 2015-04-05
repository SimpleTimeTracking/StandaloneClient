package org.stt.query;

import org.stt.model.TimeTrackingItem;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 05.04.15.
 */
public class DNFClauseMatcher {
    private DNFClause dnfClause;

    public DNFClauseMatcher(DNFClause dnfClause) {

        this.dnfClause = checkNotNull(dnfClause);
    }

    public boolean matches(TimeTrackingItem item) {
        checkNotNull(item);
        if (dnfClause.startBefore.isPresent() && !item.getStart().isBefore(dnfClause.startBefore.get())) {
            return false;
        }
        if (dnfClause.startNotBefore.isPresent() && item.getStart().isBefore(dnfClause.startNotBefore.get())) {
            return false;
        }
        if (dnfClause.endNotAfter.isPresent() && (!item.getEnd().isPresent() || item.getEnd().get().isAfter(dnfClause.endNotAfter.get()))) {
            return false;
        }
        if (dnfClause.endBefore.isPresent() && (!item.getEnd().isPresent() || !item.getEnd().get().isBefore(dnfClause.endBefore.get()))) {
            return false;
        }
        if (dnfClause.commentContains.isPresent() && (!item.getComment().isPresent() || !item.getComment().get().contains(dnfClause.commentContains.get()))) {
            return false;
        }
        return true;
    }
}
