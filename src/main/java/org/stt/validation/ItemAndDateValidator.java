package org.stt.validation;

import org.stt.model.TimeTrackingItem;
import org.stt.query.Criteria;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.time.DateTimes;
import org.stt.time.Interval;

import javax.inject.Inject;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

public class ItemAndDateValidator {
    private final TimeTrackingItemQueries timeTrackingItemQueries;

    @Inject
    public ItemAndDateValidator(TimeTrackingItemQueries timeTrackingItemQueries) {
        this.timeTrackingItemQueries = requireNonNull(timeTrackingItemQueries);
    }

    public boolean validateItemIsFirstItemAndLater(LocalDateTime start) {
        if (!DateTimes.isToday(start)) {
            return true;
        }
        LocalDateTime startOfDay = start.toLocalDate().atStartOfDay();
        Interval searchInterval = Interval.between(startOfDay, start);
        Criteria criteria = new Criteria();
        criteria.withStartBetween(searchInterval);
        boolean hasEarlierItem = timeTrackingItemQueries.queryItems(criteria).findAny().isPresent();
        if (hasEarlierItem) {
            return true;
        }
        return !LocalDateTime.now().isBefore(start);
    }

    public int validateItemWouldCoverOtherItems(TimeTrackingItem newItem) {
        Criteria criteria = new Criteria();
        criteria.withStartNotBefore(newItem.getStart());
        newItem.getEnd().ifPresent(criteria::withEndNotAfter);
        return (int) timeTrackingItemQueries.queryItems(criteria)
                .filter(item -> !newItem.getActivity().equals(item.getActivity()))
                .filter(item -> !newItem.getStart().equals(item.getStart())
                        || !newItem.getEnd().equals(item.getEnd()))
                .count();
    }

}
