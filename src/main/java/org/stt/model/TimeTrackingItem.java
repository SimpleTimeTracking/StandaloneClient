package org.stt.model;

import org.stt.time.DateTimes;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.stt.States.requireThat;


public final class TimeTrackingItem {
    private final String activity;
    private final LocalDateTime start;
    private final Optional<LocalDateTime> end;

    /**
     * @param activity activity string describing this item.
     * @param start    start time of the item
     * @param end      end time of the item.
     */
    public TimeTrackingItem(String activity, LocalDateTime start, LocalDateTime end) {
        this.activity = requireNonNull(activity);
        this.start = requireNonNull(start, "start must not be null");
        this.end = Optional.of(DateTimes.preciseToSecond(end));
        requireThat(!end.isBefore(DateTimes.preciseToSecond(start)),
                "end must not be before start for item " + this.toString());
    }

    /**
     * @param activity activity string describing this item.
     * @param start    start time of the item
     */
    public TimeTrackingItem(String activity, LocalDateTime start) {
        this.activity = requireNonNull(activity);
        this.start = DateTimes.preciseToSecond(requireNonNull(start));
        this.end = Optional.empty();
    }

    public boolean sameEndAs(TimeTrackingItem other) {
        return getEnd()
                .map(endA -> other.getEnd().map(endA::equals).orElse(false))
                .orElse(!other.getEnd().isPresent());
    }

    public boolean sameActivityAs(TimeTrackingItem other) {
        return getActivity().equals(other.getActivity());
    }

    public boolean sameStartAs(TimeTrackingItem other) {
        return getStart().equals(other.getStart());
    }

    public boolean intersects(TimeTrackingItem other) {
        return other.getEnd().map(actualEnd -> actualEnd.isAfter(start)).orElse(true)
                && end.map(actualEnd -> actualEnd.isAfter(other.start)).orElse(true);
    }

    public boolean endsSameOrAfter(TimeTrackingItem other) {
        return end.map(actualEnd -> other.getEnd().map(otherEnd -> !actualEnd.isBefore(otherEnd))
                .orElse(false))
                .orElse(true);
    }

    public boolean endsAtOrBefore(LocalDateTime dateTime) {
        return end.map(actualEnd -> !dateTime.isBefore(actualEnd))
                .orElse(false);
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
        requireNonNull(newEnd);
        return new TimeTrackingItem(activity, start, newEnd);
    }

    public TimeTrackingItem withPendingEnd() {
        return new TimeTrackingItem(activity, start);
    }

    public TimeTrackingItem withStart(LocalDateTime newStart) {
        requireNonNull(newStart);
        return end.map(time -> new TimeTrackingItem(activity, newStart, time))
                .orElse(new TimeTrackingItem(activity, newStart));
    }

    public TimeTrackingItem withActivity(String newActivity) {
        requireNonNull(newActivity);
        return end.map(time -> new TimeTrackingItem(newActivity, start, time))
                .orElse(new TimeTrackingItem(activity, start));
    }
}
