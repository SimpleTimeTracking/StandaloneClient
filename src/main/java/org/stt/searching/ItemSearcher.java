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
	 * Returns the current item that is being tracked or <i>absent</i> if none
	 * is active or available.
	 */
	Optional<TimeTrackingItem> getCurrentTimeTrackingitem();

	/**
	 * Returns the first item of the given day. If an item is active at 00:00 of
	 * the day, it is returned. If no item has been recorded for the day,
	 * <i>absent</i> is returned.
	 */
	Optional<TimeTrackingItem> getFirstItemOfDay(DateTime theDay);

	/**
	 * Returns the last item of the given day. If an item is active at 24:00 of
	 * the day, it is returned. If no item has been recorded for the day,
	 * <i>absent</i> is returned.
	 */
	Optional<TimeTrackingItem> getLastItemOfDay(DateTime theDay);

	/**
	 * Returns a list with all days that have tracking items.
	 */
	Collection<DateTime> getAllTrackedDays();
}
