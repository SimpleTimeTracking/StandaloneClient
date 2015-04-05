package org.stt.query;

import com.google.common.base.Optional;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.io.IOException;

/**
 * Reads from the given reader but only returns items where the comment contains
 * the given substring.
 * 
 * If the given substring is null, all items are returned.
 */
public class SubstringReaderFilter implements ItemReader {

	private ItemReader reader;
	private String substring;

	private boolean ignoreCase = true;

	public SubstringReaderFilter(ItemReader reader, String substring) {
		this.reader = reader;
		this.substring = substring;
	}

	public SubstringReaderFilter(ItemReader reader, String substring,
			boolean ignoreCase) {
		this.reader = reader;
		this.substring = substring;
		this.ignoreCase = ignoreCase;
	}

	@Override
	public Optional<TimeTrackingItem> read() {
		String searchString = null;
		if (substring != null) {
			searchString = ignoreCase ? substring.toLowerCase() : substring;
		}

		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			String comment = item.get().getComment().orNull();
			
			if (ignoreCase && comment != null) {
				comment = comment.toLowerCase();
			}

			if (searchString == null
					|| comment != null && comment.contains(searchString)) {
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
