package org.stt.reporting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.stt.Configuration;
import org.stt.DateTimeHelper;

public class WorkingtimeItemProvider {

	private Map<Integer, Integer> defaultWorkingHours = new HashMap<>();
	private Map<DateTime, Integer> workingHoursPerDay = new HashMap<>();

	public WorkingtimeItemProvider(Configuration configuration) {
		populateHoursMapsFromFile(configuration.getWorkingTimesFile());
	}

	public Duration getWorkingTimeFor(DateTime date) {
		Integer workingHours = workingHoursPerDay.get(date
				.withTimeAtStartOfDay());
		if (workingHours == null) {
			workingHours = defaultWorkingHours.get(date.getDayOfWeek());
		}

		return new Duration(workingHours.longValue()
				* DateTimeConstants.MILLIS_PER_HOUR);
	}

	private void populateHoursMapsFromFile(File workingTimesFile) {

		// some defaults
		defaultWorkingHours.put(DateTimeConstants.MONDAY, 8);
		defaultWorkingHours.put(DateTimeConstants.TUESDAY, 8);
		defaultWorkingHours.put(DateTimeConstants.WEDNESDAY, 8);
		defaultWorkingHours.put(DateTimeConstants.THURSDAY, 8);
		defaultWorkingHours.put(DateTimeConstants.FRIDAY, 8);
		defaultWorkingHours.put(DateTimeConstants.SATURDAY, 0);
		defaultWorkingHours.put(DateTimeConstants.SUNDAY, 0);

		// end defaults
		try (BufferedReader wtReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(workingTimesFile),
						"UTF-8"))) {

			String currentLine;
			while ((currentLine = wtReader.readLine()) != null) {
				if (currentLine.matches("^\\d+.*")) {
					// it is a date
					String[] split = currentLine.split("\\s+");
					DateTime parseDateTime = DateTimeHelper.ymdDateFormat
							.parseDateTime(split[0]);
					workingHoursPerDay.put(parseDateTime,
							Integer.parseInt(split[1]));

				} else if (currentLine.startsWith("hours")) {
					// it is the number of hours
					String[] split = currentLine.split("=");
					String spec = split[0].trim();
					String hours = split[1].trim();
					switch (spec) {
					case "hoursMon":
						defaultWorkingHours.put(DateTimeConstants.MONDAY,
								Integer.parseInt(hours));
						break;
					case "hoursTue":
						defaultWorkingHours.put(DateTimeConstants.TUESDAY,
								Integer.parseInt(hours));
						break;
					case "hoursWed":
						defaultWorkingHours.put(DateTimeConstants.WEDNESDAY,
								Integer.parseInt(hours));
						break;
					case "hoursThu":
						defaultWorkingHours.put(DateTimeConstants.THURSDAY,
								Integer.parseInt(hours));
						break;
					case "hoursFri":
						defaultWorkingHours.put(DateTimeConstants.FRIDAY,
								Integer.parseInt(hours));
						break;
					case "hoursSat":
						defaultWorkingHours.put(DateTimeConstants.SATURDAY,
								Integer.parseInt(hours));
						break;
					case "hoursSun":
						defaultWorkingHours.put(DateTimeConstants.SUNDAY,
								Integer.parseInt(hours));
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
}
