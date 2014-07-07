package org.stt.filter;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

/**
 * Reads from the given reader but only returns items where from <= start date <
 * to.
 */
public class StartDateReaderFilter implements ItemReader {

	private final ItemReader reader;
	private final DateTime from;
	private final DateTime to;

	public StartDateReaderFilter(ItemReader reader, DateTime from, DateTime to) {
		this.reader = checkNotNull(reader);
		this.from = checkNotNull(from);
		this.to = checkNotNull(to);
	}

	@Override
	public Optional<TimeTrackingItem> read() {

		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			DateTime start = item.get().getStart();
			if (to.isAfter(start) && !start.isBefore(from)) {
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
