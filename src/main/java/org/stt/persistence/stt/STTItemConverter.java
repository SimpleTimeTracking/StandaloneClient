package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

class STTItemConverter {
    private final DateTimeFormatter dateFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd_HH:mm:ss");

    public TimeTrackingItem lineToTimeTrackingItem(String line) {
        int endOfStartDate = line.indexOf(' ');
        endOfStartDate = endOfStartDate < 0 ? line.length() : endOfStartDate;
        LocalDateTime start = LocalDateTime.parse(line.substring(0, endOfStartDate), dateFormat);
        LocalDateTime end = null;
        int endOfEndDate = line.indexOf(' ', endOfStartDate + 1);
        if (endOfEndDate >= 0) {
            try {
                end = LocalDateTime.parse(line.substring(endOfStartDate + 1, endOfEndDate), dateFormat);
            } catch (DateTimeParseException e) { // NOPMD
                // It was no end date after all
                endOfEndDate = endOfStartDate;
            }
        } else {
            endOfEndDate = endOfStartDate;
        }
        String activity = endOfEndDate < line.length() ? line
                .substring(endOfEndDate + 1)
                .replace("\\n", "\n")
                : "";
        if (end != null) {
            return new TimeTrackingItem(activity, start, end);
        }
        return new TimeTrackingItem(activity, start);
    }

    public String timeTrackingItemToLine(TimeTrackingItem item) {
        StringBuilder builder = new StringBuilder();
        builder.append(item.getStart().format(dateFormat));
        builder.append(' ');
        item.getEnd()
                .ifPresent(endDateTime -> {
                    builder.append(endDateTime.format(dateFormat));
                    builder.append(' ');
                });

        String oneLineComment = item.getActivity();
        oneLineComment = oneLineComment.replaceAll("\r\n|\r|\n", "\\\\n");
        builder.append(oneLineComment);

        return builder.toString();
    }
}
