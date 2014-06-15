package org.stt.filter;

import java.io.IOException;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

/**
 * Reads from the given reader but only returns items where the comment contains
 * the given substring.
 * 
 * If the given substring is null, all items are returned.
 */
public class SubstringReaderFilter implements ItemReader {

	private ItemReader reader;
	private String substring;

	public SubstringReaderFilter(ItemReader reader, String substring) {
		this.reader = reader;
		this.substring = substring;
	}

	@Override
	public Optional<TimeTrackingItem> read() {

		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			String comment = item.get().getComment().orNull();
			if (substring == null
					|| (comment != null && comment.contains(substring))) {
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
