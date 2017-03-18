package org.stt.query;

import org.stt.model.TimeTrackingItem;
import org.stt.time.Interval;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Disjunctive normal form term. All present conditions in the clause must be valid for a match.
 */
public class Criteria {
    private LocalDateTime startNotBefore = LocalDateTime.MIN;
    private LocalDateTime startBefore = LocalDateTime.MAX;
    private LocalDateTime endNotAfter = LocalDateTime.MAX;
    private LocalDateTime endBefore = LocalDateTime.MAX;
    private String commentContains = "";

    public Criteria withStartBetween(Interval interval) {
        Objects.requireNonNull(interval);
        withStartNotBefore(interval.getStart());
        withStartBefore(interval.getEnd());
        return this;
    }

    public Criteria withStartNotBefore(LocalDateTime time) {
        startNotBefore = time;
        return this;
    }

    public Criteria withStartBefore(LocalDateTime time) {
        startBefore = time;
        return this;
    }

    public Criteria withEndNotAfter(LocalDateTime time) {
        endNotAfter = time;
        return this;
    }

    public Criteria withPeriodAtDay(LocalDate date) {
        LocalDateTime startOfDayAtDate = date.atStartOfDay();
        withStartNotBefore(startOfDayAtDate);
        withStartBefore(startOfDayAtDate.plusDays(1));
        return this;
    }

    public Criteria withEndBefore(LocalDateTime time) {
        endBefore = time;
        return this;
    }

    public Criteria withCommentContains(String substring) {
        commentContains = substring;
        return this;
    }

    public boolean matches(TimeTrackingItem item) {
        Objects.requireNonNull(item);
        if (!item.getStart().isBefore(startBefore)) {
            return false;
        }
        if (item.getStart().isBefore(startNotBefore)) {
            return false;
        }
        if (item.getEnd().orElse(LocalDateTime.MAX).isAfter(endNotAfter)) {
            return false;
        }
        if (!item.getEnd().orElse(LocalDateTime.MIN).isBefore(endBefore)) {
            return false;
        }
        if (!item.getActivity().contains(commentContains)) {
            return false;
        }
        return true;
    }
}
