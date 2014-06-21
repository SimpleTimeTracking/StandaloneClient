package org.stt.searching;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.stt.filter.SubstringReaderFilter;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;

public class TimeTrackingItemScanningSearcher implements CommentSearcher {
	private ItemReaderProvider itemReaderProvider;
	private static final Logger LOG = Logger
			.getLogger(TimeTrackingItemScanningSearcher.class.getName());

	@Override
	public Collection<String> searchForComments(String partialComment) {
		try (ItemReader reader = itemReaderProvider.provideReader();
				SubstringReaderFilter filteredReader = new SubstringReaderFilter(
						reader, partialComment)) {
		} catch (IOException e) {
			LOG.log(Level.SEVERE,
					"An error occured while scanning for TimeTrackingItems", e);
			return Collections.emptyList();
		}
		return null;
	}

}
