package org.stt.persistence;

import java.util.Collection;

import org.joda.time.ReadableInstant;
import org.stt.model.TimeTrackingItem;

public interface ItemSearcher {

	/**
	 * Get all items for start time between from and to, both inclusive. Either
	 * from or to must not be null.
	 * 
	 * @param from
	 *            if null, get all items before to
	 * @param to
	 *            if null, get all items after from
	 */
	Collection<TimeTrackingItem> searchByStart(ReadableInstant from,
			ReadableInstant to);

	/**
	 * Get all items for end time between from and to, both inclusive. If from
	 * and to are both null, returns only items without end time.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	Collection<TimeTrackingItem> searchByEnd(ReadableInstant from,
			ReadableInstant to);

	/**
	 * Get all items where the comment matches the given search string.
	 * 
	 * @param search
	 * @return
	 */
	Collection<TimeTrackingItem> searchByComment(String search);
}
