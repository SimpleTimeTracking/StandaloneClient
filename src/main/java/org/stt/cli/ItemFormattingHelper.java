package org.stt.cli;

import org.stt.model.TimeTrackingItem;
import org.stt.time.DateTimes;

import java.time.Duration;
import java.time.LocalDateTime;

class ItemFormattingHelper {

    private ItemFormattingHelper() {
    }

    static String prettyPrintItem(TimeTrackingItem item) {

        StringBuilder builder = new StringBuilder();

        LocalDateTime start = item.getStart();
        LocalDateTime end = item.getEnd().orElse(null);
        String comment = item.getActivity();

        builder.append(DateTimes.prettyPrintTime(start));

        builder.append(" - ");
        if (end == null) {
            builder.append("now     ");
        } else {
            builder.append(DateTimes.DATE_TIME_FORMATTER_HH_MM_SS.format(end));
        }
        builder.append(" ( ");
        builder.append(DateTimes.FORMATTER_PERIOD_HHh_MMm_SSs
                .print(Duration.between(start, end == null ? LocalDateTime.now()
                        : end)));
        builder.append(" ) ");
        builder.append(" => ");
        builder.append(comment);

        return builder.toString();
    }
}
