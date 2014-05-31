package org.stt;

import org.stt.model.TimeTrackingItem;

/**
 * @author bytekeeper
 * 
 */
public interface CommandHandler {
	/**
	 * Executes a command, ie. add a {@link TimeTrackingItem}.
	 * 
	 * @param command
	 */
	void executeCommand(String command);
}
