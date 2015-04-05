package org.stt.validation;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.stt.model.TimeTrackingItem;
import org.stt.query.DNFClause;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.time.DateTimeHelper;

/**
 * Created by dante on 16.03.15.
 */
public class ItemAndDateValidator {
    private final TimeTrackingItemQueries timeTrackingItemQueries;

    @Inject
    public ItemAndDateValidator(TimeTrackingItemQueries timeTrackingItemQueries) {
        this.timeTrackingItemQueries = timeTrackingItemQueries;
    }

    public boolean validateItemIsFirstItemAndLater(DateTime start) {
        DateTime startOfDay = start.withTimeAtStartOfDay();
        Interval searchInterval = new Interval(startOfDay, start);
        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartBetween(searchInterval);
        boolean hasEarlierItem = !timeTrackingItemQueries.queryItems(dnfClause).isEmpty();
        return !(DateTimeHelper.isToday(start) && DateTime.now().plusMinutes(5).isBefore(start)
                && !hasEarlierItem);
    }

    public int validateItemWouldCoverOtherItems(TimeTrackingItem newItem) {
        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartNotBefore(newItem.getStart());
        if (newItem.getEnd().isPresent()) {
            dnfClause.withEndNotAfter(newItem.getEnd().get());
        }
        int numberOfCoveredItems = 0;
        for (TimeTrackingItem item: timeTrackingItemQueries.queryItems(dnfClause)) {
            if (!newItem.getComment().equals(item.getComment()) && (!newItem.getStart().equals(item.getStart())
                    || !newItem.getEnd().equals(item.getEnd()))) {
                numberOfCoveredItems++;
            }
        }
        return numberOfCoveredItems;
    }

}
