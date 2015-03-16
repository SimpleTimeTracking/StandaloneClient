package org.stt.validation;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.stt.model.TimeTrackingItem;
import org.stt.search.ItemSearcher;
import org.stt.search.Query;
import org.stt.time.DateTimeHelper;

/**
 * Created by dante on 16.03.15.
 */
public class ItemAndDateValidator {
    private final ItemSearcher itemSearcher;

    @Inject
    public ItemAndDateValidator(ItemSearcher itemSearcher) {
        this.itemSearcher = itemSearcher;
    }

    public boolean validateItemIsFirstItemAndLater(DateTime start) {
        DateTime startOfDay = start.withTimeAtStartOfDay();
        Interval searchInterval = new Interval(startOfDay, start);
        Query query = new Query();
        query.withStartBetween(searchInterval);
        boolean hasEarlierItem = !itemSearcher.queryItems(query).isEmpty();
        return !(DateTimeHelper.isToday(start) && DateTime.now().plusMinutes(5).isBefore(start)
                && !hasEarlierItem);
    }

    public int validateItemWouldCoverOtherItems(TimeTrackingItem newItem) {
        Query query = new Query();
        query.withStartNotBefore(newItem.getStart());
        if (newItem.getEnd().isPresent()) {
            query.withEndNotAfter(newItem.getEnd().get());
        }
        return itemSearcher.queryItems(query).size();
    }

}
