package org.stt.search;

import java.util.Collection;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

public interface ItemSearcher {
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
