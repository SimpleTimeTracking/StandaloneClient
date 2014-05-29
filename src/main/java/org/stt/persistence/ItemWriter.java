package org.stt.persistence;

import java.util.Calendar;

public interface ItemWriter {
	/**
	 * Inserts a new interval. If existing intervals are completely covered,
	 * they will be removed, otherwise they will be cropped as needed.
	 */
	void writeItemWithDurationAtAndOverwriteCoveredIntervals(
			Calendar dateAndTime, Duration duration, String description);

	/**
	 * Inserts a new interval. If existing intervals are completely covered,
	 * they will be retained, otherwise they will be cropped as needed.
	 */
	void writeItemWithDurationAndAndRetainCoveredIntervals(
			Calendar dateAndTime, Duration duration, String description);

	/**
	 * Inserts the item at the given date and time. The item before and after
	 * will not be changed, therefore the new item "ends" the item before and
	 * ends itself when the next one starts.
	 */
	void writeItemAt(Calendar dateAndTime, String description);
}
