package org.stt.reporting;

import com.google.common.base.Optional;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.analysis.ItemCategorizer;
import org.stt.analysis.ItemCategorizer.ItemCategory;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem;
import org.stt.time.DateTimeHelper;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calculates overtime information
 */
public class OvertimeReportGenerator {

	private final ItemCategorizer categorizer;
	private final ItemReaderProvider readerProvider;
	private WorkingtimeItemProvider workingtimeItemProvider;

	public OvertimeReportGenerator(ItemReaderProvider readerProvider,
			ItemCategorizer categorizer,
			WorkingtimeItemProvider workingtimeItemProvider) {
		this.readerProvider = readerProvider;
		this.categorizer = categorizer;
		this.workingtimeItemProvider = workingtimeItemProvider;
	}

	/**
	 * @return overtime information from the given time to the other given time
	 */
	public Map<DateTime, Duration> getOvertime(DateTime from, DateTime to) {
		Map<DateTime, Duration> result = new TreeMap<>();

		for (Map.Entry<DateTime, Duration> e : getOvertime().entrySet()) {
			if (DateTimeHelper.isBetween(e.getKey(), from, to)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}

	public Duration getOverallOvertime() {
		Duration result = new Duration(0);
		for (Duration d : getOvertime().values()) {
			result = result.plus(d);
		}
		return result;
	}

	/**
	 * @return the date and the according overtime (positive or negative) for
	 *         all elements
	 */
	public Map<DateTime, Duration> getOvertime() {

		Optional<TimeTrackingItem> optionalItem = null;

		try (ItemReader reader = readerProvider.provideReader()) {
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

			dateToOvertime.putAll(getAbsencesMap());
			return dateToOvertime;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * returns the dates and corresponding absence durations for the given date
	 */
	private Map<DateTime, Duration> getAbsencesMap() {
		Map<DateTime, WorkingtimeItem> overtimeAbsencesSince = workingtimeItemProvider
				.getOvertimeAbsences();
		Map<DateTime, Duration> resultMap = new TreeMap<>();
		for (Map.Entry<DateTime, WorkingtimeItem> e : overtimeAbsencesSince
				.entrySet()) {
			resultMap.put(e.getKey(), e.getValue().getMin());
		}
		return resultMap;
	}

	/**
	 * returns the overtime (positive or negative) for the given date and
	 * duration
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
