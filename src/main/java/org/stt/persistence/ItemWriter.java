package org.stt.persistence;

import java.io.IOException;

import org.stt.model.TimeTrackingItem;

public interface ItemWriter {
	
	/**
	 * @param item the item to write. If it already exists, it will be overwritten so the caller has to take care
	 * @throws IOException
	 */
	void write(TimeTrackingItem item) throws IOException;
	
	/**
	 * @param item the item to delete. If the item does not already exist, just does nothing
	 * @throws IOException
	 */
	void delete(TimeTrackingItem item) throws IOException;
	
	/**
	 * Closes the Writer and all underlying streams if any exist.
	 * Exceptions are not passed on but caught and handled internally.
	 */
	void close();
}
