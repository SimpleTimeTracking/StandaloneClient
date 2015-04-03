package org.stt.reporting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.stt.Configuration;
import org.stt.time.DateTimeHelper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads information about working times from the configured workingTimes file
 * and aggregates them into {@link WorkingtimeItem}s
 */
@Singleton
public class WorkingtimeItemProvider {

	private Map<Integer, WorkingtimeItem> defaultWorkingHours = new HashMap<>();
	private Map<DateTime, WorkingtimeItem> workingHoursPerDay = new HashMap<>();

	@Inject
	public WorkingtimeItemProvider(Configuration configuration) {
		populateHoursMapsFromFile(configuration.getWorkingTimesFile());
	}

	/**
	 * 
	 * @return the dates and corresponding absence times
	 */
	public Map<DateTime, WorkingtimeItem> getOvertimeAbsences() {
		Map<DateTime, WorkingtimeItem> overtimeAbsenceDuration = new TreeMap<>();
		for (Map.Entry<DateTime, WorkingtimeItem> e : workingHoursPerDay
				.entrySet()) {
			// if time is negative...
			if (e.getValue().getMin().isShorterThan(new Duration(0))) {
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
		WorkingtimeItem workingHours = workingHoursPerDay.get(date
				.toDateTimeAtStartOfDay());
		if (workingHours == null) {
			workingHours = defaultWorkingHours.get(date.getDayOfWeek());
		}

		return workingHours;
	}

	private void populateHoursMapsFromFile(File workingTimesFile) {

		// some defaults
		defaultWorkingHours.put(DateTimeConstants.MONDAY, fromHours("8"));
		defaultWorkingHours.put(DateTimeConstants.TUESDAY, fromHours("8"));
		defaultWorkingHours.put(DateTimeConstants.WEDNESDAY, fromHours("8"));
		defaultWorkingHours.put(DateTimeConstants.THURSDAY, fromHours("8"));
		defaultWorkingHours.put(DateTimeConstants.FRIDAY, fromHours("8"));
		defaultWorkingHours.put(DateTimeConstants.SATURDAY, fromHours("8"));
		defaultWorkingHours.put(DateTimeConstants.SUNDAY, fromHours("0"));

		// end defaults
		try (BufferedReader wtReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(workingTimesFile),
						"UTF-8"))) {

			String currentLine;
			while ((currentLine = wtReader.readLine()) != null) {
				if (currentLine.matches("^\\d+.*")) {
					// it is a date
					String[] split = currentLine.split("\\s+");
					DateTime parseDateTime = DateTimeHelper.DATE_TIME_FORMATTER_YYYY_MM_DD
							.parseDateTime(split[0]);
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
						defaultWorkingHours.put(DateTimeConstants.MONDAY,
								fromHours(hours));
						break;
					case "hoursTue":
						defaultWorkingHours.put(DateTimeConstants.TUESDAY,
								fromHours(hours));
						break;
					case "hoursWed":
						defaultWorkingHours.put(DateTimeConstants.WEDNESDAY,
								fromHours(hours));
						break;
					case "hoursThu":
						defaultWorkingHours.put(DateTimeConstants.THURSDAY,
								fromHours(hours));
						break;
					case "hoursFri":
						defaultWorkingHours.put(DateTimeConstants.FRIDAY,
								fromHours(hours));
						break;
					case "hoursSat":
						defaultWorkingHours.put(DateTimeConstants.SATURDAY,
								fromHours(hours));
						break;
					case "hoursSun":
						defaultWorkingHours.put(DateTimeConstants.SUNDAY,
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
		Duration minDur = new Duration(Long.parseLong(minHours)
				* DateTimeConstants.MILLIS_PER_HOUR);
		Duration maxDur = new Duration(Long.parseLong(maxHours)
				* DateTimeConstants.MILLIS_PER_HOUR);
		return new WorkingtimeItem(minDur, maxDur);
	}

	private WorkingtimeItem fromHours(String hours) {
		Duration d = new Duration(Long.parseLong(hours)
				* DateTimeConstants.MILLIS_PER_HOUR);
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
