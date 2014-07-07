package org.stt.reporting;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.reporting.ItemCategorizer.ItemCategory;
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem;

import com.google.common.base.Optional;

public class OvertimeReportGenerator {

	private final ItemCategorizer categorizer;
	private final ItemReader reader;
	private WorkingtimeItemProvider workingtimeItemProvider;

	public OvertimeReportGenerator(ItemReader reader,
			ItemCategorizer categorizer,
			WorkingtimeItemProvider workingtimeItemProvider) {
		this.reader = reader;
		this.categorizer = categorizer;
		this.workingtimeItemProvider = workingtimeItemProvider;

	}

	public Map<DateTime, Duration> getOvertime() {

		Optional<TimeTrackingItem> optionalItem = null;

		Map<DateTime, Duration> dateToOvertime = new TreeMap<>();
		while ((optionalItem = reader.read()).isPresent()) {
			TimeTrackingItem item = optionalItem.get();

			ItemCategory category = categorizer.getCategory(item.getComment()
					.orNull());
			if (category.equals(ItemCategory.WORKTIME)) {
				DateTime currentDay = item.getStart().withTimeAtStartOfDay();
				Duration currentDuration = dateToOvertime.get(currentDay);
				Duration itemDuration = new Duration(item.getStart(), item
						.getEnd().or(DateTime.now()));
				if (currentDuration != null) {
					dateToOvertime.put(currentDay,
							currentDuration.plus(itemDuration));
				} else {
					dateToOvertime.put(currentDay, itemDuration);
				}
			}
		}

		for (Map.Entry<DateTime, Duration> e : dateToOvertime.entrySet()) {
			dateToOvertime.put(e.getKey(),
					getOvertime(e.getKey(), e.getValue()));
		}
		IOUtils.closeQuietly(reader);

		return dateToOvertime;
	}

	/**
	 * returns the configured daily working hours
	 */
	private Duration getOvertime(DateTime date, Duration duration) {
		WorkingtimeItem workingTimeForDate = workingtimeItemProvider
				.getWorkingTimeFor(date);
		if (duration.isLongerThan(workingTimeForDate.getMax())) {
			return duration.minus(workingTimeForDate.getMax());
		} else if (duration.isShorterThan(workingTimeForDate.getMin())) {
			return duration.minus(workingTimeForDate.getMin());
		}

		return new Duration(0);
	}
}
