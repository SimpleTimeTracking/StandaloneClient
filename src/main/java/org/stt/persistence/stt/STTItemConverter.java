package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class STTItemConverter {
    private static final DateTimeFormatter dateFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd_HH:mm:ss");

    TimeTrackingItem lineToTimeTrackingItem(String line) {
        LocalDateTime start = parseDate(line.substring(0, 19));
        LocalDateTime end = line.length() >= 39 ? parseDate(line.substring(20, 39)) : null;
        int activityStart = end == null ? 20 : 40;
        String activity = line.length() > activityStart ?
                unescape(line.substring(activityStart)) : "";
        if (end != null) {
            return new TimeTrackingItem(activity, start, end);
        }
        return new TimeTrackingItem(activity, start);
    }

    private String unescape(String activity) {
        char[] chars = activity.toCharArray();
        int n = chars.length;
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < n) {
            char next = chars[i];
            if (next == '\\' && i + 1 < n) {
                i++;
                if (chars[i] == 'n') {
                    b.append('\n');
                } else {
                    b.append('\\').append(chars[i]);
                }
            } else {
                b.append(next);
            }
            i++;
        }
        return b.toString();
    }

    private LocalDateTime parseDate(String from) {
        char[] chars = from.toCharArray();
        if (chars[0] < '0' || chars[0] > '9'
                || chars[1] < '0' || chars[1] > '9'
                || chars[2] < '0' || chars[2] > '9'
                || chars[3] < '0' || chars[3] > '9'
                || chars[4] != '-'
                || chars[5] < '0' || chars[5] > '9'
                || chars[6] < '0' || chars[6] > '9'
                || chars[7] != '-'
                || chars[8] < '0' || chars[8] > '9'
                || chars[9] < '0' || chars[9] > '9'
                || chars[10] != '_'
                || chars[11] < '0' || chars[11] > '9'
                || chars[12] < '0' || chars[12] > '9'
                || chars[13] != ':'
                || chars[14] < '0' || chars[14] > '9'
                || chars[15] < '0' || chars[15] > '9'
                || chars[16] != ':'
                || chars[17] < '0' || chars[17] > '9'
                || chars[18] < '0' || chars[18] > '9') {
            return null;
        }
        int y = (chars[0] - '0') * 1000 + (chars[1] - '0') * 100 + (chars[2] - '0') * 10 + (chars[3] - '0');
        int mo = (chars[5] - '0') * 10 + (chars[6] - '0');
        int d = (chars[8] - '0') * 10 + (chars[9] - '0');
        int h = (chars[11] - '0') * 10 + (chars[12] - '0');
        int mi = (chars[14] - '0') * 10 + (chars[15] - '0');
        int s = (chars[17] - '0') * 10 + (chars[18] - '0');
        return LocalDateTime.of(y, mo, d, h, mi, s);
    }

    String timeTrackingItemToLine(TimeTrackingItem item) {
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
