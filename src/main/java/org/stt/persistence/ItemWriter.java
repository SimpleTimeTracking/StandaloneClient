package org.stt.persistence;

import java.io.IOException;

import org.stt.model.TimeTrackingItem;

public interface ItemWriter {
	void write(TimeTrackingItem item) throws IOException;
}
