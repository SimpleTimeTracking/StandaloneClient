package org.stt.importer;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class IOUtil {

	public static Collection<TimeTrackingItem> readAll(ItemReader reader)
			throws IOException {
		Collection<TimeTrackingItem> result = new ArrayList<>();
		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			result.add(item.get());
		}
		reader.close();
		return result;
	}
}
