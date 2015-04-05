package org.stt.query;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads from the given reader but only returns items where from <= start date <
 * to.
 */
public class StartDateReaderFilter implements ItemReader {

	private final ItemReader reader;
	private final QueryMatcher queryMatcher;

	public StartDateReaderFilter(ItemReader reader, Query query) {
		this.reader = checkNotNull(reader);
		this.queryMatcher = new QueryMatcher(checkNotNull(query));
	}

	@Override
	public Optional<TimeTrackingItem> read() {

		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			if (queryMatcher.matches(item.get())) {
				return item;
			}
		}
		return Optional.absent();
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

}
