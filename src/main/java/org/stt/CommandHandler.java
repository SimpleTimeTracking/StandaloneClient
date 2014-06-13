package org.stt;

import java.io.Closeable;

import org.stt.model.TimeTrackingItem;

/**
 * @author bytekeeper
 * 
 */
public interface CommandHandler extends Closeable {
	/**
	 * Executes a command, ie. add a {@link TimeTrackingItem}.
	 * 
	 * @param command
	 * @return the created item or null if no item has been created
	 */
	TimeTrackingItem executeCommand(String command);
}
