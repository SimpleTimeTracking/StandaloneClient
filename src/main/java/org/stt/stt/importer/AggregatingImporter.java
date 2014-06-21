package org.stt.stt.importer;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

/**
 * Aggregates multiple importers and reads elements of all importers sequentially.
 * The first importer given in the constructor is read first, then the second and so on. 
 */
public class AggregatingImporter implements ItemReader {

	private ItemReader[] readers;
	
	public AggregatingImporter(ItemReader... readers) {
		this.readers = readers;
	}
	
	@Override
	public void close() throws IOException {
		for(ItemReader r : readers) {
			IOUtils.closeQuietly(r);
		}
	}

	@Override
	public Optional<TimeTrackingItem> read() {
		for(ItemReader r : readers) {
			Optional<TimeTrackingItem> optionalItem = r.read();
			if(optionalItem.isPresent()) {
				return optionalItem;
			}
		}
		return Optional.<TimeTrackingItem> absent();
	}

}
