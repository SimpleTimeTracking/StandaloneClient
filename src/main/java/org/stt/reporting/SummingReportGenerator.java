package org.stt.reporting;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

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

	public Report report() {
		DateTime startOfReport = null;
		DateTime endOfReport = null;
		List<ReportingItem> reportList = new LinkedList<>();

		Map<String, Duration> collectingMap = new HashMap<>();

		Optional<TimeTrackingItem> optionalItem = null;
		while ((optionalItem = reader.read()).isPresent()) {
			TimeTrackingItem item = optionalItem.get();
			DateTime end = item.getEnd().or(DateTime.now());

			if (startOfReport == null) {
				startOfReport = item.getStart();
			}
			endOfReport = end;

			Duration duration = new Duration(item.getStart(), end);
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
		return new Report(reportList, startOfReport, endOfReport);
	}

	public static class Report {

		private final List<ReportingItem> reportingItems;
		private final DateTime start;
		private final DateTime end;

		public Report(List<ReportingItem> reportingItems, DateTime start,
				DateTime end) {
			super();
			this.reportingItems = reportingItems;
			this.start = start;
			this.end = end;
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
	}
}
