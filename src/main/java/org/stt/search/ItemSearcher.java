package org.stt.search;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Interval;
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

	/**
	 * Returns the first maxItems (if absent, all) items that are within the interval [start,end].
	 * If start is absent, -&infin; is assumed, if end is absent, +&infin; is assumed.
	 * @return
	 */
	Collection<TimeTrackingItem> getFirstNItems(Optional<DateTime> start, Optional<DateTime> end, Optional<Integer> maxItems);

    Collection<TimeTrackingItem> queryItems(Query query);
}
