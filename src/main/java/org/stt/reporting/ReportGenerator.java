package org.stt.reporting;

import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class ReportGenerator {

	private ItemReader reader;

	public ReportGenerator(ItemReader reader) {
		this.reader = reader;
	}

	public Set<ReportingItem> report() {

		HashSet<ReportingItem> reportList = new HashSet<>();

		Optional<TimeTrackingItem> optionalItem = null;
		while ((optionalItem = reader.read()).isPresent()) {
			TimeTrackingItem item = optionalItem.get();

			DateTime end = item.getEnd().or(DateTime.now());
			Duration duration = new Duration(item.getStart(), end);
			ReportingItem repItem = new ReportingItem(duration, item
					.getComment().orNull());

			if (reportList.remove(repItem)) {
				reportList.add(repItem.addDurationOf(repItem));
			} else {
				reportList.add(repItem);
			}
		}

		return reportList;
	}
}
