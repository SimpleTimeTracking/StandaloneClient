package org.stt.persistence;

import org.stt.model.TimeTrackingItem;

import java.io.Closeable;
import java.io.IOException;

public interface ItemWriter extends Closeable {
	void write(TimeTrackingItem item) throws IOException;
}
