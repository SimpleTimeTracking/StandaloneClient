package org.stt.search;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.stt.filter.SubstringReaderFilter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;

public class TimeTrackingItemScanningSearcher implements CommentSearcher {
	private final ItemReaderProvider itemReaderProvider;
	private static final Logger LOG = Logger
			.getLogger(TimeTrackingItemScanningSearcher.class.getName());

	public TimeTrackingItemScanningSearcher(
			ItemReaderProvider itemReaderProvider) {
		this.itemReaderProvider = checkNotNull(itemReaderProvider);
	}

	@Override
	public Collection<String> searchForComments(String partialComment) {
		List<String> result = new ArrayList<>();
		try (ItemReader reader = itemReaderProvider.provideReader();
				SubstringReaderFilter filteredReader = new SubstringReaderFilter(
						reader, partialComment)) {
			Optional<TimeTrackingItem> read;
			while ((read = filteredReader.read()).isPresent()) {
				TimeTrackingItem item = read.get();
				result.add(item.getComment().get());
			}
			Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
			String last = null;
			for (Iterator<String> it = result.iterator(); it.hasNext();) {
				String current = it.next();
				if (last != null && last.equals(current)) {
					it.remove();
				}
				last = current;
			}
		} catch (IOException e) {
			LOG.log(Level.SEVERE,
					"An error occured while scanning for TimeTrackingItems", e);
			return Collections.emptyList();
		}
		return result;
	}

}
