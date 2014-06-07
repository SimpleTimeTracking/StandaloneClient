package org.stt.searching;

import java.util.Collection;
import java.util.LinkedList;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemSearcher;

import com.google.common.base.Optional;

public class DefaultItemSearcher implements ItemSearcher {

	private ItemReader reader;

	/**
	 * @param reader
	 *            where to search for items
	 */
	public DefaultItemSearcher(ItemReader reader) {
		this.reader = reader;
	}

	@Override
	public Collection<TimeTrackingItem> searchByStart(ReadableInstant from,
			ReadableInstant to) {

		Collection<TimeTrackingItem> foundElements = new LinkedList<>();
		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			TimeTrackingItem currentItem = item.get();
			if (!currentItem.getStart().isBefore(from)
					&& !currentItem.getStart().isAfter(to)) {
				foundElements.add(currentItem);
			}
		}
		return foundElements;
	}

	// FIXME: incomplete and tests missing
	@Override
	public Collection<TimeTrackingItem> searchByEnd(ReadableInstant from,
			ReadableInstant to) {

		Collection<TimeTrackingItem> foundElements = new LinkedList<>();
		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			TimeTrackingItem currentItem = item.get();
			DateTime currentEnd = currentItem.getEnd().orNull();

			if (from == null && to == null && currentEnd == null) {
				foundElements.add(currentItem);

			} else if (from == null && currentEnd != null) {

			} else if (to == null) {

			} else if (!currentItem.getEnd().get().isBefore(from)
					&& !currentItem.getEnd().get().isAfter(to)) {
				foundElements.add(currentItem);
			}
		}
		return foundElements;
	}

	@Override
	public Collection<TimeTrackingItem> searchByComment(String search) {
		// TODO Auto-generated method stub
		return null;
	}

}
