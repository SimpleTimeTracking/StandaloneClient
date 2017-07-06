package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;

import java.time.LocalDateTime;

class STTItemConverter {
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
        StringBuilder builder = new StringBuilder(80);
        LocalDateTime start = item.getStart();
        appendDateTime(builder, start);
        builder.append(' ');
        item.getEnd()
                .ifPresent(endDateTime -> {
                    appendDateTime(builder, endDateTime);
                    builder.append(' ');
                });

        escape(builder, item.getActivity());
        return builder.toString();
    }

    private void escape(StringBuilder b, String activity) {
        char[] chars = activity.toCharArray();
        int n = chars.length;
        int i = 0;
        while (i < n) {
            char next = chars[i];
            if (next == '\r') {
                b.append("\\n");
                if (i + 1 < n && chars[i + 1] == '\n') {
                    i++;
                }
            } else if (next == '\n') {
                b.append("\\n");
            } else {
                b.append(next);
            }
            i++;
        }
    }

    private void appendDateTime(StringBuilder b, LocalDateTime dt) {
        int y = dt.getYear();
        int mo = dt.getMonthValue();
        int d = dt.getDayOfMonth();
        int h = dt.getHour();
        int mi = dt.getMinute();
        int s = dt.getSecond();
        b.append((char) (y / 1000 % 10 + '0'));
        b.append((char) (y / 100 % 10 + '0'));
        b.append((char) (y / 10 % 10 + '0'));
        b.append((char) (y % 10 + '0'));
        b.append('-');
        b.append((char) (mo / 10 % 10 + '0'));
        b.append((char) (mo % 10 + '0'));
        b.append('-');
        b.append((char) (d / 10 % 10 + '0'));
        b.append((char) (d % 10 + '0'));
        b.append('_');
        b.append((char) (h / 10 % 10 + '0'));
        b.append((char) (h % 10 + '0'));
        b.append(':');
        b.append((char) (mi / 10 % 10 + '0'));
        b.append((char) (mi % 10 + '0'));
        b.append(':');
        b.append((char) (s / 10 % 10 + '0'));
        b.append((char) (s % 10 + '0'));
    }
}
