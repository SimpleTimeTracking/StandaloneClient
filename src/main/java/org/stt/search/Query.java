package org.stt.search;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.swing.text.html.Option;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 16.03.15.
 */
public class Query {
    Optional<DateTime> startNotBefore = Optional.absent();
    Optional<DateTime> startBefore = Optional.absent();
    Optional<DateTime> endNotAfter = Optional.absent();

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
}
