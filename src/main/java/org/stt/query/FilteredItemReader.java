package org.stt.query;

import com.google.common.base.Optional;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads from the given reader but only returns items where from <= start date <
 * to.
 */
public class FilteredItemReader implements ItemReader {

	private final ItemReader reader;
	private final DNFClauseMatcher DNFClauseMatcher;

	public FilteredItemReader(ItemReader reader, DNFClause dnfClause) {
		this.reader = checkNotNull(reader);
		this.DNFClauseMatcher = new DNFClauseMatcher(checkNotNull(dnfClause));
	}

	@Override
	public Optional<TimeTrackingItem> read() {

		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			if (DNFClauseMatcher.matches(item.get())) {
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
