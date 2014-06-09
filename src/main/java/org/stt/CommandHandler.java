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
	 */
	void executeCommand(String command);
}
