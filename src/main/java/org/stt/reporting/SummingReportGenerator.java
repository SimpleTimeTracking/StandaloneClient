package org.stt.reporting;

import com.google.common.base.Optional;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads all elements from the given reader and groups by the comment of the
 * item: all items with the identical comment get merged into one
 * {@link ReportingItem}. Duration is the sum of all durations of the items.
 *
 * Items without an end date get reported as if the end date was now
 *
 * If a comment of an item is null, it will be treated as the empty String.
 *
 * Items will be returned sorted in ascending order of the comments
 */
public class SummingReportGenerator {

	private final ItemReader reader;

	public SummingReportGenerator(ItemReader reader) {
		this.reader = reader;
	}

	public Report createReport() {
		DateTime startOfReport = null;
		DateTime endOfReport = null;
		List<ReportingItem> reportList = new LinkedList<>();

		Map<String, Duration> collectingMap = new HashMap<>();

		Duration uncoveredDuration = Duration.ZERO;
		Optional<TimeTrackingItem> optionalItem;
		TimeTrackingItem lastItem = null;
		while ((optionalItem = reader.read()).isPresent()) {
			TimeTrackingItem item = optionalItem.get();
			DateTime now = DateTime.now();
			DateTime start = item.getStart();
			DateTime end = item.getEnd().or(now);

			if (lastItem != null) {
				DateTime endOfLastItem = lastItem.getEnd().or(now);
				if (endOfLastItem.isBefore(start)) {
					Duration additionalUncoveredTime = new Duration(
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

			Duration duration = new Duration(start, end);
			String comment = item.getComment().or("");
			if (collectingMap.containsKey(comment)) {
				Duration oldDuration = collectingMap.get(comment);
				collectingMap.put(comment, oldDuration.plus(duration));
			} else {
				collectingMap.put(comment, duration);
			}
		}

		IOUtils.closeQuietly(reader);

		for (Map.Entry<String, Duration> e : collectingMap.entrySet()) {
			reportList.add(new ReportingItem(e.getValue(), e.getKey()));
		}

		Collections.sort(reportList, new Comparator<ReportingItem>() {

			@Override
			public int compare(ReportingItem o1, ReportingItem o2) {
				return o1.getComment().compareTo(o2.getComment());
			}
		});
		return new Report(reportList, startOfReport, endOfReport,
				uncoveredDuration);
	}

	public static class Report {

		private final List<ReportingItem> reportingItems;
		private final DateTime start;
		private final DateTime end;
		private final Duration uncoveredDuration;

		public Report(List<ReportingItem> reportingItems, DateTime start,
				DateTime end, Duration uncoveredDuration) {
			this.reportingItems = reportingItems;
			this.start = start;
			this.end = end;
			this.uncoveredDuration = checkNotNull(uncoveredDuration);
		}

		public List<ReportingItem> getReportingItems() {
			return reportingItems;
		}

		public DateTime getStart() {
			return start;
		}

		public DateTime getEnd() {
			return end;
		}

		public Duration getUncoveredDuration() {
			return uncoveredDuration;
		}
	}
}
