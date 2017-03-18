package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class STTItemConverter {
    private final DateTimeFormatter dateFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd_HH:mm:ss");

    public TimeTrackingItem lineToTimeTrackingItem(String singleLine) {

        List<String> splitLine = new LinkedList<>(Arrays.asList(singleLine
                .split(" ")));

        LocalDateTime start = LocalDateTime.parse(splitLine.remove(0), dateFormat);

        LocalDateTime end = null;
        if (!splitLine.isEmpty()) {
            try {
                end = LocalDateTime.parse(splitLine.get(0), dateFormat);
                splitLine.remove(0);
            } catch (DateTimeParseException i) { // NOPMD
                // NOOP, if the string cannot be parsed, it is no date
                // this is a bit ugly but currently no idea how to do it
                // "correctly"
            }
        }
        String comment = "";
        if (!splitLine.isEmpty()) {
            StringBuilder commentBuilder = new StringBuilder(
                    singleLine.length());
            for (String current : splitLine) {
                current = current.replaceAll("\\\\r", "\r");
                current = current.replaceAll("\\\\n", "\n");
                commentBuilder.append(current);

                commentBuilder.append(" ");
            }
            commentBuilder.deleteCharAt(commentBuilder.length() - 1);

            comment = commentBuilder.toString();
        }

        if (end != null) {
            return new TimeTrackingItem(comment, start, end);
        } else {
            return new TimeTrackingItem(comment, start);
        }
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
        oneLineComment = oneLineComment.replaceAll("\r", "\\\\r");
        oneLineComment = oneLineComment.replaceAll("\n", "\\\\n");
        builder.append(oneLineComment);

        return builder.toString();
    }
}
