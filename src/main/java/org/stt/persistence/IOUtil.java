package org.stt.persistence;

import java.util.ArrayList;
import java.util.Collection;

import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

public class IOUtil {

	public static Collection<TimeTrackingItem> readAll(ItemReader reader) {
		Collection<TimeTrackingItem> result = new ArrayList<>();
		Optional<TimeTrackingItem> item;
		while ((item = reader.read()).isPresent()) {
			result.add(item.get());
		}
		reader.close();
		return result;
	}
}
