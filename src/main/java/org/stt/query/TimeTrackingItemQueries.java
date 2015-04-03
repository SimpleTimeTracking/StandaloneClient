package org.stt.query;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;

import java.util.Collection;

public interface TimeTrackingItemQueries {
	/**
	 * Returns the current item that is being tracked or <i>absent</i> if none
	 * is active or available.
	 */
	Optional<TimeTrackingItem> getCurrentTimeTrackingitem();

	/**
	 * Returns a list with all days that have tracking items.
	 */
	Collection<DateTime> getAllTrackedDays();

	/**
	 * Returns the first maxItems (if absent, all) items that are within the interval [start,end].
	 * If start is absent, -&infin; is assumed, if end is absent, +&infin; is assumed.
	 * @return
	 */
	Collection<TimeTrackingItem> queryFirstNItems(Optional<DateTime> start, Optional<DateTime> end, Optional<Integer> maxItems);

    Collection<TimeTrackingItem> queryItems(Query query);

    Collection<TimeTrackingItem> queryAllItems();
}
