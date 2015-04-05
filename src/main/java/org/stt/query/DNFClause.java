package org.stt.query;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Disjunctive normal form term. All present conditions in the clause must be valid for a match.
 */
public class DNFClause {
    Optional<DateTime> startNotBefore = Optional.absent();
    Optional<DateTime> startBefore = Optional.absent();
    Optional<DateTime> endNotAfter = Optional.absent();
    Optional<DateTime> endBefore = Optional.absent();
    Optional<String> commentContains = Optional.absent();

    public DNFClause withStartBetween(Interval interval) {
        checkNotNull(interval);
        withStartNotBefore(interval.getStart());
        withStartBefore(interval.getEnd());
        return this;
    }

    public DNFClause withStartNotBefore(DateTime time) {
        startNotBefore = Optional.of(time);
        return this;
    }

    public DNFClause withStartBefore(DateTime time) {
        startBefore = Optional.of(time);
        return this;
    }

    public DNFClause withEndNotAfter(DateTime time) {
        endNotAfter = Optional.of(time);
        return this;
    }

    public DNFClause withPeriodAtDay(LocalDate date) {
        DateTime startOfDayAtDate = date.toDateTimeAtStartOfDay();
        withStartNotBefore(startOfDayAtDate);
        withStartBefore(startOfDayAtDate.plusDays(1));
        return this;
    }

    public DNFClause withEndBefore(DateTime time) {
        endBefore = Optional.of(time);
        return this;
    }

    public DNFClause withCommentContains(String substring) {
        commentContains = Optional.of(substring);
        return this;
    }
}
