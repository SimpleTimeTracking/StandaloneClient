package org.stt.search;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultItemSearcher implements ItemSearcher {

	private final ItemReaderProvider provider;

	/**
	 * @param provider
	 *            where to search for items
	 */
	@Inject
	public DefaultItemSearcher(ItemReaderProvider provider) {
		this.provider = checkNotNull(provider);
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
}
