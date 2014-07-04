package org.stt.persistence;

import java.io.Closeable;
import java.io.IOException;

import org.stt.model.TimeTrackingItem;

public interface ItemWriter extends Closeable {
	void write(TimeTrackingItem item) throws IOException;
}
