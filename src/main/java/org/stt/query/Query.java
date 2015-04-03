package org.stt.query;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 16.03.15.
 */
public class Query {
    Optional<DateTime> startNotBefore = Optional.absent();
    Optional<DateTime> startBefore = Optional.absent();
    Optional<DateTime> endNotAfter = Optional.absent();
    Optional<DateTime> endBefore = Optional.absent();

    public Query withStartBetween(Interval interval) {
        checkNotNull(interval);
        withStartNotBefore(interval.getStart());
        withStartBefore(interval.getEnd());
        return this;
    }

    public Query withStartNotBefore(DateTime time) {
        startNotBefore = Optional.of(time);
        return this;
    }

    public Query withStartBefore(DateTime time) {
        startBefore = Optional.of(time);
        return this;
    }

    public Query withEndNotAfter(DateTime time) {
        endNotAfter = Optional.of(time);
        return this;
    }

    public Query withPeriodAtDay(LocalDate date) {
        DateTime startOfDayAtDate = date.toDateTimeAtStartOfDay();
        withStartNotBefore(startOfDayAtDate);
        withStartBefore(startOfDayAtDate.plusDays(1));
        return this;
    }

    public Query withEndBefore(DateTime time) {
        endBefore = Optional.of(time);
        return this;
    }
}
