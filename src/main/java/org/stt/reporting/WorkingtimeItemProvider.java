package org.stt.reporting;

import org.stt.Configuration;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import java.io.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Reads information about working times from the configured workingTimes file
 * and aggregates them into {@link WorkingtimeItem}s
 */
public class WorkingtimeItemProvider {
    private Map<DayOfWeek, WorkingtimeItem> defaultWorkingHours;
    private Map<LocalDate, WorkingtimeItem> workingHoursPerDay = new HashMap<>();

	@Inject
	public WorkingtimeItemProvider(Configuration configuration) {
		populateHoursMapsFromFile(configuration.getWorkingTimesFile());
	}

	/**
	 * 
	 * @return the dates and corresponding absence times
	 */
    public Map<LocalDate, WorkingtimeItem> getOvertimeAbsences() {
        Map<LocalDate, WorkingtimeItem> overtimeAbsenceDuration = new TreeMap<>();
        for (Map.Entry<LocalDate, WorkingtimeItem> e : workingHoursPerDay
                .entrySet()) {
			// if time is negative...
            if (e.getValue().getMin().isNegative()) {
                overtimeAbsenceDuration.put(e.getKey(), e.getValue());
			}
		}

		return overtimeAbsenceDuration;
	}

	/**
	 * 
	 * @param date
	 * @return the configured duration to be worked without producing positive
	 *         or negative overtime
	 */
	public WorkingtimeItem getWorkingTimeFor(LocalDate date) {
        WorkingtimeItem workingHours = workingHoursPerDay.get(date);
        if (workingHours == null) {
			workingHours = defaultWorkingHours.get(date.getDayOfWeek());
		}

		return workingHours;
	}

	private void populateHoursMapsFromFile(File workingTimesFile) {

		// some defaults
        defaultWorkingHours = Arrays.stream(DayOfWeek.values())
                .collect(
                        toMap(identity(),
                                dayOfWeek -> dayOfWeek != DayOfWeek.SUNDAY
                                        ? fromHours("8") : fromHours("0")));

		// end defaults
		try (BufferedReader wtReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(workingTimesFile),
						"UTF-8"))) {

			String currentLine;
			while ((currentLine = wtReader.readLine()) != null) {
				if (currentLine.matches("^\\d+.*")) {
					// it is a date
					String[] split = currentLine.split("\\s+");
                    LocalDate parseDateTime = LocalDate.parse(split[0], DateTimes.DATE_TIME_FORMATTER_YYYY_MM_DD_DASHED);
                    String minHours = split[1];
                    String maxHours = minHours;

					if (split.length > 2) {
						maxHours = split[2];
					}
					workingHoursPerDay.put(parseDateTime,
							fromHours(minHours, maxHours));

				} else if (currentLine.startsWith("hours")) {
					// it is the number of hours
					String[] split = currentLine.split("=");
					String spec = split[0].trim();
					String hours = split[1].trim();
					switch (spec) {
					case "hoursMon":
                        defaultWorkingHours.put(DayOfWeek.MONDAY,
                                fromHours(hours));
						break;
					case "hoursTue":
                        defaultWorkingHours.put(DayOfWeek.TUESDAY,
                                fromHours(hours));
						break;
					case "hoursWed":
                        defaultWorkingHours.put(DayOfWeek.WEDNESDAY,
                                fromHours(hours));
						break;
					case "hoursThu":
                        defaultWorkingHours.put(DayOfWeek.THURSDAY,
                                fromHours(hours));
						break;
					case "hoursFri":
                        defaultWorkingHours.put(DayOfWeek.FRIDAY,
                                fromHours(hours));
						break;
					case "hoursSat":
                        defaultWorkingHours.put(DayOfWeek.SATURDAY,
                                fromHours(hours));
						break;
					case "hoursSun":
                        defaultWorkingHours.put(DayOfWeek.SUNDAY,
                                fromHours(hours));
						break;

					default:
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private WorkingtimeItem fromHours(String minHours, String maxHours) {
        Duration minDur = Duration.ofHours(Long.parseLong(minHours));
        Duration maxDur = Duration.ofHours(Long.parseLong(maxHours));
        return new WorkingtimeItem(minDur, maxDur);
	}

	private WorkingtimeItem fromHours(String hours) {
        Duration d = Duration.ofHours(Long.parseLong(hours));
        return new WorkingtimeItem(d, d);
	}

	/**
	 *  
	 */
	public static class WorkingtimeItem {

		private Duration min;
		private Duration max;

		public WorkingtimeItem(Duration min, Duration max) {
			this.min = min;
			this.max = max;
		}

		public Duration getMin() {
			return min;
		}

		public Duration getMax() {
			return max;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((max == null) ? 0 : max.hashCode());
			result = prime * result + ((min == null) ? 0 : min.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			WorkingtimeItem other = (WorkingtimeItem) obj;
			if (max == null) {
				if (other.max != null) {
					return false;
				}
			} else if (!max.equals(other.max)) {
				return false;
			}
			if (min == null) {
				if (other.min != null) {
					return false;
				}
			} else if (!min.equals(other.min)) {
				return false;
			}
			return true;
		}
	}
}
