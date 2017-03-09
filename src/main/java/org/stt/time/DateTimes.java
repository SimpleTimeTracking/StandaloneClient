package org.stt.time;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimes {
    public static final DateTimeFormatter DATE_TIME_FORMATTER_HH_MM_SS = DateTimeFormatter
            .ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS = DateTimeFormatter
            .ofPattern("yyyy.MM.dd HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME_FORMATTER_YYYY_MM_DD_DASHED = DateTimeFormatter
            .ofPattern("yyyy-MM-dd");

    public static final DurationPrinter FORMATTER_PERIOD_HHh_MMm_SSs = duration -> {
        long seconds = duration.getSeconds();
        return String.format("%d:%02d:%02d",
                seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    };

    public static final DurationPrinter FORMATTER_PERIOD_H_M_S = duration -> {
        long seconds = duration.getSeconds();
        return String.format("%d:%d:%d",
                seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    };

    private DateTimes() {
    }


    public static boolean isToday(LocalDateTime date) {
        return LocalDate.now().equals(date.toLocalDate());
    }

    public static boolean isToday(LocalDate date) {
        return LocalDate.now().equals(date);
    }

    public static boolean isOnSameDay(LocalDateTime a, LocalDateTime b) {
        return a.toLocalDate().equals(b.toLocalDate());
    }

    /**
     * @param source
     * @param from
     * @param to
     * @return if source is between from and to (both inclusive)
     */
    public static boolean isBetween(LocalDate source, LocalDate from, LocalDate to) {
        return !source.isBefore(from) && !source.isAfter(to);
    }

    /**
     * returns the formatted date in asNewItemCommandText "HH:mm:ss" if the given date is
     * today, "yyyy-MM-dd HH:mm:ss" if the given date is not today
     */
    public static String prettyPrintTime(LocalDateTime date) {
        if (isToday(date)) {
            return DATE_TIME_FORMATTER_HH_MM_SS.format(date);
        } else {
            return DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS.format(date);
        }
    }

    /**
     * returns the formatted date in asNewItemCommandText "yyyy-MM-dd"
     *
     * @param date
     */
    public static String prettyPrintDate(LocalDate date) {
        return DATE_TIME_FORMATTER_YYYY_MM_DD_DASHED.format(date);
    }

    public static String prettyPrintDuration(Duration duration) {
        if (duration.isNegative()) {
            // it is negative
            return "-"
                    + FORMATTER_PERIOD_HHh_MMm_SSs.print(duration.abs());
        } else {
            return " " + FORMATTER_PERIOD_HHh_MMm_SSs.print(duration);
        }
    }
}
