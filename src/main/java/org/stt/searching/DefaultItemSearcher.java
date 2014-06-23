package org.stt.searching;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;

import com.google.common.base.Optional;

public class DefaultItemSearcher implements ItemSearcher {

	private final ItemReaderProvider provider;

	/**
	 * @param reader
	 *            where to search for items
	 */
	public DefaultItemSearcher(ItemReaderProvider provider) {
		this.provider = checkNotNull(provider);
	}

	@Override
	public Collection<TimeTrackingItem> searchByStart(ReadableInstant from,
			ReadableInstant to) {

		Collection<TimeTrackingItem> foundElements = new LinkedList<>();
		Optional<TimeTrackingItem> item;
		try (ItemReader reader = provider.provideReader()) {
			while ((item = reader.read()).isPresent()) {
				TimeTrackingItem currentItem = item.get();
				if (!currentItem.getStart().isBefore(from)
						&& !currentItem.getStart().isAfter(to)) {
					foundElements.add(currentItem);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return foundElements;
	}

	@Override
	public Optional<TimeTrackingItem> getCurrentTimeTrackingitem() {
		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> item;
			TimeTrackingItem currentItem = null;
			while ((item = reader.read()).isPresent()) {
				currentItem = item.get();
			}
			return currentItem == null || currentItem.getEnd().isPresent() ? Optional
					.<TimeTrackingItem> absent() : Optional.of(currentItem);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<DateTime> getAllTrackedDays() {
		Collection<DateTime> result = new ArrayList<>();
		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> item;
			DateTime lastDay = null;
			while ((item = reader.read()).isPresent()) {
				DateTime currentDay = item.get().getStart()
						.withTimeAtStartOfDay();
				if (lastDay == null || !lastDay.equals(currentDay)) {
					result.add(currentDay);
					lastDay = currentDay;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	public Optional<TimeTrackingItem> getFirstItemOfDay(DateTime theDay) {
		Optional<TimeTrackingItem> resultingItem = Optional.absent();

		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> item;
			while ((item = reader.read()).isPresent()) {

				if (!isOnSameDay(theDay, item.get().getStart())) {
					continue;
				}

				if (!resultingItem.isPresent()
						|| resultingItem.get().getStart()
								.isAfter(item.get().getStart())) {

					resultingItem = item;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return resultingItem;
	}

	@Override
	public Optional<TimeTrackingItem> getLastItemOfDay(DateTime theDay) {
		Optional<TimeTrackingItem> resultingItem = Optional.absent();

		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> item;
			while ((item = reader.read()).isPresent()) {

				if (!isOnSameDay(theDay, item.get().getStart())) {
					continue;
				}

				if (!resultingItem.isPresent()
						|| resultingItem.get().getStart()
								.isBefore(item.get().getStart())) {
					resultingItem = item;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return resultingItem;
	}

	private boolean isOnSameDay(DateTime d1, DateTime d2) {
		boolean sameYear = d1.getYear() == d2.getYear();
		boolean sameDay = d1.getDayOfYear() == d2.getDayOfYear();

		return sameYear && sameDay;
	}
}
