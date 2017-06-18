package org.stt.reporting;

import org.stt.config.WorktimeConfig;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Reads information about working times from the configured workingTimes file
 * and aggregates them into {@link WorkingtimeItem}s
 */
public class WorkingtimeItemProvider {
    private static final Logger LOG = Logger.getLogger(WorkingtimeItemProvider.class.getSimpleName());
    private final WorktimeConfig config;
    private Map<LocalDate, WorkingtimeItem> workingHoursPerDay = new HashMap<>();

	@Inject
    public WorkingtimeItemProvider(WorktimeConfig config,
                                   @Named("homePath") String homePath) {
        this.config = requireNonNull(config);

        File workingTimesFile = config.getWorkingTimesFile().file(homePath);
        if (workingTimesFile.exists()) {
            populateHoursMapsFromFile(workingTimesFile);
        }
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
            return fromDuration(config.getWorkingHours().getOrDefault(date.getDayOfWeek(), Duration.ZERO));
        }
        return workingHours;
    }

	private void populateHoursMapsFromFile(File workingTimesFile) {

		try (BufferedReader wtReader = new BufferedReader(
				new InputStreamReader(constructReaderFrom(workingTimesFile), StandardCharsets.UTF_8))) {

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
                    LOG.severe("'hours' is no longer supported in your worktimes file, please setup default working hours in your configuration.");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

	private InputStream constructReaderFrom(File workingTimesFile) throws FileNotFoundException {
		if(workingTimesFile.getName().equalsIgnoreCase("-")) {
			return System.in;
		}
		return new FileInputStream(workingTimesFile);
	}

	private WorkingtimeItem fromHours(String minHours, String maxHours) {
        Duration minDur = Duration.ofHours(Long.parseLong(minHours));
        Duration maxDur = Duration.ofHours(Long.parseLong(maxHours));
        return new WorkingtimeItem(minDur, maxDur);
	}

    private WorkingtimeItem fromDuration(Duration duration) {
        return new WorkingtimeItem(duration, duration);
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
