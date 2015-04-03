package org.stt.validation;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.query.Query;
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
        Query query = new Query();
        query.withStartBetween(searchInterval);
        boolean hasEarlierItem = !timeTrackingItemQueries.queryItems(query).isEmpty();
        return !(DateTimeHelper.isToday(start) && DateTime.now().plusMinutes(5).isBefore(start)
                && !hasEarlierItem);
    }

    public int validateItemWouldCoverOtherItems(TimeTrackingItem newItem) {
        Query query = new Query();
        query.withStartNotBefore(newItem.getStart());
        if (newItem.getEnd().isPresent()) {
            query.withEndNotAfter(newItem.getEnd().get());
        }
        int numberOfCoveredItems = 0;
        for (TimeTrackingItem item: timeTrackingItemQueries.queryItems(query)) {
            if (!newItem.getComment().equals(item.getComment()) && (!newItem.getStart().equals(item.getStart())
                    || !newItem.getEnd().equals(item.getEnd()))) {
                numberOfCoveredItems++;
            }
        }
        return numberOfCoveredItems;
    }

}
