package org.stt.persistence;

import com.google.common.base.Optional;
import org.stt.model.TimeTrackingItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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
