package org.stt;

import java.io.Closeable;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

/**
 * @author bytekeeper
 * 
 */
public interface CommandHandler extends Closeable {
	/**
	 * Executes a command, ie. add a {@link TimeTrackingItem}.
	 * 
	 * @param command
	 * @return the created/updated item or {@link Optional#absent()}
	 */
	Optional<TimeTrackingItem> executeCommand(String command);

	void endCurrentItem();

	void resumeGivenItem(TimeTrackingItem item);

	/**
	 * Sets the end time for the currently active item. If no active item
	 * exists, does nothing.
	 */
	Optional<TimeTrackingItem> endCurrentItem(
			DateTime startTimeOfNewItem);
}
