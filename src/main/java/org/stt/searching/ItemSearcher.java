package org.stt.searching;

import java.util.Collection;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

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
	 * Returns the current item that is being tracked or <i>absent</i> if none
	 * is active or available.
	 */
	Optional<TimeTrackingItem> getCurrentTimeTrackingitem();

	/**
	 * Returns a list with all days that have tracking items.
	 */
	Collection<DateTime> getAllTrackedDays();
}
