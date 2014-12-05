package org.stt.search;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;

import java.util.Collection;

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
