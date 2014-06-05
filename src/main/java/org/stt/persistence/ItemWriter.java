package org.stt.persistence;

import org.stt.model.TimeTrackingItem;

public interface ItemWriter {
	
	void write(TimeTrackingItem item);
	
	void delete(TimeTrackingItem item);
}
