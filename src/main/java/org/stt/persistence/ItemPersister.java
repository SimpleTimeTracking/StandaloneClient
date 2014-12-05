package org.stt.persistence;

import org.stt.model.TimeTrackingItem;

import java.io.Closeable;
import java.io.IOException;

public interface ItemPersister extends Closeable {

	/**
	 * Writes the given item.
	 * <p>
	 * If the new item has no end time:
	 * <ul>
	 * <li>If the item.start is before any other item's start time, the existing
	 * items will be removed.</li>
	 * <li>If an item is not ended yet when the new item is written, it's end
	 * time will be set to the new item's start time.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param item
	 *            the item to write. If it already exists, it will be
	 *            overwritten so the caller has to take care
	 * @throws IOException
	 */
	void insert(TimeTrackingItem item) throws IOException;

	/**
	 * Replaces the given item with a new one.
	 * <p>
	 * This is equivalent to calling {@link #delete(TimeTrackingItem)} and
	 * {@link #insert(TimeTrackingItem)} but may potentially be faster
	 * </p>
	 */
	void replace(TimeTrackingItem item, TimeTrackingItem with)
			throws IOException;

	/**
	 * @param item
	 *            the item to delete. If the item does not already exist, just
	 *            does nothing
	 * @throws IOException
	 */
	void delete(TimeTrackingItem item) throws IOException;
}
