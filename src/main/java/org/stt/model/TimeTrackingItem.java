package org.stt.model;

import org.stt.time.DateTimes;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.stt.States.requireThat;


public final class TimeTrackingItem {
    private final String activity;
    private final LocalDateTime start;
    private final LocalDateTime end;

    /**
     * @param activity activity string describing this item.
     * @param start    start time of the item
     * @param end      end time of the item.
     */
    public TimeTrackingItem(String activity, LocalDateTime start, LocalDateTime end) {
        this.activity = requireNonNull(activity);
        this.start = DateTimes.preciseToSecond(requireNonNull(start, "start must not be null"));
        this.end = DateTimes.preciseToSecond(requireNonNull(end, "end must not be null"));
        requireThat(!end.isBefore(start),
                "end must not be before start for item!");
    }

    /**
     * @param activity activity string describing this item.
     * @param start    start time of the item
     */
    public TimeTrackingItem(String activity, LocalDateTime start) {
        this.activity = requireNonNull(activity);
        this.start = DateTimes.preciseToSecond(requireNonNull(start));
        this.end = null;
    }

    public boolean sameEndAs(TimeTrackingItem other) {
        return end == other.end
                || end != null && end.equals(other.end);
    }

    public boolean sameActivityAs(TimeTrackingItem other) {
        return getActivity().equals(other.getActivity());
    }

    public boolean sameStartAs(TimeTrackingItem other) {
        return getStart().equals(other.getStart());
    }

    public boolean intersects(TimeTrackingItem other) {
        return (end == null || end.isAfter(other.start))
                && (other.end == null || other.end.isAfter(start));
    }

    public boolean endsSameOrAfter(TimeTrackingItem other) {
        return end == null || other.end != null && !end.isBefore(other.end);
    }

    public boolean endsAtOrBefore(LocalDateTime dateTime) {
        return end != null && !dateTime.isBefore(end);
    }

    public String getActivity() {
        return activity;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public Optional<LocalDateTime> getEnd() {
        return Optional.ofNullable(end);
    }


    @Override
    public String toString() {
        return start.toString() + " - "
                + (end == null ? "null" : end.toString()) + " : "
                + activity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeTrackingItem that = (TimeTrackingItem) o;
        return Objects.equals(activity, that.activity) &&
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activity, start, end);
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
        return end != null ? new TimeTrackingItem(activity, newStart, end)
                : new TimeTrackingItem(activity, newStart);
    }

    public TimeTrackingItem withActivity(String newActivity) {
        requireNonNull(newActivity);
        return end != null ? new TimeTrackingItem(newActivity, start, end)
                : new TimeTrackingItem(newActivity, start);
    }
}
