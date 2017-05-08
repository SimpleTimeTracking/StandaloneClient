package org.stt.reporting;

import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

/**
 * Reads all elements from the given reader and groups by the comment of the
 * item: all items with the identical comment get merged into one
 * {@link ReportingItem}. Duration is the sum of all durations of the items.
 * <p>
 * Items without an end date get reported as if the end date was now
 * <p>
 * Items will be returned sorted in ascending order of the comments
 */
public class SummingReportGenerator {

    private final Stream<TimeTrackingItem> itemsToRead;

    public SummingReportGenerator(Stream<TimeTrackingItem> itemsToRead) {
        this.itemsToRead = itemsToRead;
    }

    public Report createReport() {
        LocalDateTime startOfReport = null;
        LocalDateTime endOfReport = null;
        List<ReportingItem> reportList = new LinkedList<>();


        Map<String, Duration> collectingMap = new HashMap<>();
        Duration uncoveredDuration = Duration.ZERO;
        TimeTrackingItem lastItem = null;
        try (Stream<TimeTrackingItem> items = itemsToRead) {
            for (Iterator<TimeTrackingItem> it = items.iterator(); it.hasNext(); ) {
                TimeTrackingItem item = it.next();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime start = item.getStart();
                LocalDateTime end = item.getEnd().orElse(now);

                if (lastItem != null) {
                    LocalDateTime endOfLastItem = lastItem.getEnd().orElse(now);
                    if (endOfLastItem.isBefore(start)) {
                        Duration additionalUncoveredTime = Duration.between(
                                endOfLastItem, start);
                        uncoveredDuration = uncoveredDuration
                                .plus(additionalUncoveredTime);
                    }
                }

                lastItem = item;

                if (startOfReport == null) {
                    startOfReport = start;
                }
                endOfReport = end;

                Duration duration = Duration.between(start, end);
                if (duration.isNegative()) {
                    duration = Duration.ZERO;
                }
                String comment = item.getActivity();
                if (collectingMap.containsKey(comment)) {
                    Duration oldDuration = collectingMap.get(comment);
                    collectingMap.put(comment, oldDuration.plus(duration));
                } else {
                    collectingMap.put(comment, duration);
                }
            }

        }

        for (Map.Entry<String, Duration> e : collectingMap.entrySet()) {
            reportList.add(new ReportingItem(e.getValue(), e.getKey()));
        }

        Collections.sort(reportList, comparing(ReportingItem::getComment));
        return new Report(reportList, startOfReport, endOfReport,
                uncoveredDuration);
    }


    public static class Report {
        private final List<ReportingItem> reportingItems;
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final Duration uncoveredDuration;

        public Report(List<ReportingItem> reportingItems, LocalDateTime start,
                      LocalDateTime end, Duration uncoveredDuration) {
            this.reportingItems = reportingItems;
            this.start = start;
            this.end = end;
            this.uncoveredDuration = Objects.requireNonNull(uncoveredDuration);
        }

        public List<ReportingItem> getReportingItems() {
            return reportingItems;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public LocalDateTime getEnd() {
            return end;
        }

        public Duration getUncoveredDuration() {
            return uncoveredDuration;
        }
    }
}
