package org.stt.reporting;

import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.Configuration;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.reporting.ItemCategorizer.ItemCategory;

import com.google.common.base.Optional;

/**
 * @author tw
 * @date 29.06.2014, 17:40:54
 */
public class OvertimeReportGenerator {

	private final ItemCategorizer categorizer;
	private final Configuration configuration;
	private final ItemReader reader;

	public OvertimeReportGenerator(ItemReader reader,
			ItemCategorizer categorizer, Configuration configuration) {
		this.reader = reader;
		this.categorizer = categorizer;
		this.configuration = configuration;

	}

	public Duration getOvertime() {
		int dailyWorkingHours = configuration.getDailyWorkingHours();

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
					dateToOvertime.put(currentDay, itemDuration
							.minus(dailyWorkingHours * 60 * 60 * 1000));
				}
			}
		}

		Duration result = new Duration(0);
		for (Duration d : dateToOvertime.values()) {
			result = result.plus(d);
		}
		return result;
	}
}
