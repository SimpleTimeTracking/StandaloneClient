package org.stt.persistence;

import java.util.Calendar;

public interface ItemWriter {
	/**
	 * Inserts the item at the given date and time with the given duration.
	 * <p>
	 * If the new time block falls between an existing one, the old "task" will
	 * be split up, ending with the start of the new item and starting again
	 * after the duration has passed.
	 * </p>
	 * <p>
	 * <i>To Be Designed</i> If the time block completely overlaps a previous
	 * block, it will be split up: It will end with the start of the first
	 * overlapping block, and begin once again with the end of the last
	 * overlapping block.
	 * </p>
	 */
	void writeItemWithDurationAt(Calendar dateAndTime, Duration duration,
			String description);

	/**
	 * Inserts the item at the given date and time. The item before and after
	 * will not be changed, therefore the new item "ends" the item before and
	 * ends itself when the next one starts.
	 */
	void writeItemAt(Calendar dateAndTime, String description);
}
