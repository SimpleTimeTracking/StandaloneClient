package org.stt;

import java.io.Closeable;

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
}
