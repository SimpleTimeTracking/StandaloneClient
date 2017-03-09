package org.stt.model;


import java.time.Duration;
import java.util.Objects;

public class ReportingItem {
    private final Duration duration;
    private final String comment;

    public ReportingItem(Duration duration, String comment) {
        this.duration = Objects.requireNonNull(duration);
        this.comment = Objects.requireNonNull(comment);
    }

    public Duration getDuration() {
        return duration;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return duration.toString() + " " + comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReportingItem that = (ReportingItem) o;

        if (!duration.equals(that.duration)) return false;
        return comment.equals(that.comment);
    }

    @Override
    public int hashCode() {
        int result = duration.hashCode();
        result = 31 * result + comment.hashCode();
        return result;
    }
}
