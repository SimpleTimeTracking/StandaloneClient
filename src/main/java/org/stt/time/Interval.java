package org.stt.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Interval {
    private final LocalDateTime start;
    private final LocalDateTime end;

    private Interval(LocalDateTime start, LocalDateTime end) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        this.start = start;
        this.end = end;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public static Interval between(LocalDateTime start, LocalDateTime end) {
        return new Interval(start, end);
    }

    public static Interval ofDay(LocalDate day) {
        return new Interval(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    public static Interval between(LocalDate start, LocalDate end) {
        return new Interval(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    public Interval withEnd(LocalDateTime newEnd) {
        return new Interval(start, newEnd);
    }
}
