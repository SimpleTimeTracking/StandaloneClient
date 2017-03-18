package org.stt.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.stt.States.requireThat;


public final class TimeTrackingItem {
    private final String activity;
    private final LocalDateTime start;
    private final Optional<LocalDateTime> end;

    /**
     * @param activity activity string describing this item. May be null
     * @param start    start time of the item
     * @param end      end time of the item.
     */
    public TimeTrackingItem(String activity, LocalDateTime start, LocalDateTime end) {
        this.activity = Objects.requireNonNull(activity);
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Optional.of(end);
        requireThat(!end.isBefore(start),
                "end must not be before start for item " + this.toString());
    }

    /**
     * @param activity activity string describing this item. May be null
     * @param start    start time of the item
     */
    public TimeTrackingItem(String activity, LocalDateTime start) {
        this.activity = Objects.requireNonNull(activity);
        this.start = start;
        this.end = Optional.empty();
    }

    public String getActivity() {
        return activity;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public Optional<LocalDateTime> getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return start.toString() + " - "
                + (end.isPresent() ? end.get().toString() : "null") + " : "
                + activity;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeTrackingItem that = (TimeTrackingItem) o;

        if (!activity.equals(that.activity)) return false;
        if (!start.equals(that.start)) return false;
        return end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = activity.hashCode();
        result = 31 * result + start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    public TimeTrackingItem withEnd(LocalDateTime newEnd) {
        Objects.requireNonNull(newEnd);
        return new TimeTrackingItem(activity, start, newEnd);
    }

    public TimeTrackingItem withPendingEnd() {
        return new TimeTrackingItem(activity, start);
    }

    public TimeTrackingItem withStart(LocalDateTime newStart) {
        Objects.requireNonNull(newStart);
        return end.map(time -> new TimeTrackingItem(activity, newStart, time))
                .orElse(new TimeTrackingItem(activity, newStart));
    }
}
