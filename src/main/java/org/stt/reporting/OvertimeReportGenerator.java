package org.stt.reporting;

import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem;
import org.stt.text.ItemCategorizer;
import org.stt.text.ItemCategorizer.ItemCategory;
import org.stt.time.DateTimes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Calculates overtime information
 */
public class OvertimeReportGenerator {

	private final ItemCategorizer categorizer;
    private final TimeTrackingItemQueries queries;
    private WorkingtimeItemProvider workingtimeItemProvider;

    public OvertimeReportGenerator(TimeTrackingItemQueries queries,
                                   ItemCategorizer categorizer,
                                   WorkingtimeItemProvider workingtimeItemProvider) {
        this.queries = queries;
        this.categorizer = categorizer;
		this.workingtimeItemProvider = workingtimeItemProvider;
	}

	/**
	 * @return overtime information from the given time to the other given time
     * @param from
     * @param to
     */
    public Map<LocalDate, Duration> getOvertime(LocalDate from, LocalDate to) {
        Map<LocalDate, Duration> result = new TreeMap<>();

        for (Map.Entry<LocalDate, Duration> e : getOvertime().entrySet()) {
            if (DateTimes.isBetween(e.getKey(), from, to)) {
                result.put(e.getKey(), e.getValue());
            }
        }
		return result;
	}

	public Duration getOverallOvertime() {
        Duration result = Duration.ZERO;
        for (Duration d : getOvertime().values()) {
			result = result.plus(d);
		}
		return result;
	}

	/**
	 * @return the date and the according overtime (positive or negative) for
	 *         all elements
	 */
    public Map<LocalDate, Duration> getOvertime() {
        try (Stream<TimeTrackingItem> items = queries.queryAllItems()) {
            Map<LocalDate, Duration> dateToOvertime = new TreeMap<>();
            items.forEach(item -> {
                ItemCategory category = categorizer.getCategory(item.getActivity());
                if (category.equals(ItemCategory.WORKTIME)) {
                    LocalDate currentDay = item.getStart().toLocalDate();
                    Duration currentDuration = dateToOvertime.get(currentDay);
                    Duration itemDuration = Duration.between(item.getStart(), item
                            .getEnd().orElse(LocalDateTime.now()));
                    if (currentDuration != null) {
						dateToOvertime.put(currentDay,
								currentDuration.plus(itemDuration));
					} else {
						dateToOvertime.put(currentDay, itemDuration);
					}
				}
            });

            for (Map.Entry<LocalDate, Duration> e : dateToOvertime.entrySet()) {
                dateToOvertime.put(e.getKey(),
						getOvertime(e.getKey(), e.getValue()));
			}

			dateToOvertime.putAll(getAbsencesMap());
			return dateToOvertime;
		}
	}

	/**
	 * returns the dates and corresponding absence durations for the given date
	 */
    private Map<LocalDate, Duration> getAbsencesMap() {
        Map<LocalDate, WorkingtimeItem> overtimeAbsencesSince = workingtimeItemProvider
                .getOvertimeAbsences();
        Map<LocalDate, Duration> resultMap = new TreeMap<>();
        for (Map.Entry<LocalDate, WorkingtimeItem> e : overtimeAbsencesSince
                .entrySet()) {
			resultMap.put(e.getKey(), e.getValue().getMin());
		}
		return resultMap;
	}

	/**
	 * returns the overtime (positive or negative) for the given date and
	 * duration
	 */
    private Duration getOvertime(LocalDate date, Duration duration) {
        WorkingtimeItem workingTimeForDate = workingtimeItemProvider
                .getWorkingTimeFor(date);
        if (duration.compareTo(workingTimeForDate.getMax()) > 0) {
            return duration.minus(workingTimeForDate.getMax());
        } else if (duration.compareTo(workingTimeForDate.getMin()) < 0) {
            return duration.minus(workingTimeForDate.getMin());
		}

        return Duration.ZERO;
    }
}
