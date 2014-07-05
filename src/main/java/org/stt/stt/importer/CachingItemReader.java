package org.stt.stt.importer;

import java.io.IOException;
import java.util.ArrayList;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class CachingItemReader implements ItemReader {

	private ArrayList<Optional<TimeTrackingItem>> cachingList = new ArrayList<>();

	private int currentListIndex = -1;

	private ItemReader input;

	public CachingItemReader(ItemReader input) {
		this.input = input;
	}

	@Override
	public void close() throws IOException {
		input.close();
	}

	@Override
	public Optional<TimeTrackingItem> read() {
		Optional<TimeTrackingItem> readItem;
		if (currentListIndex == -1) {
			readItem = input.read();
			cachingList.add(readItem);
		} else {
			readItem = cachingList.get(currentListIndex++);
		}
		if (!readItem.isPresent()) {
			currentListIndex = 0;
		}
		return readItem;
	}

}
