package org.stt.persistence;

import org.stt.model.TimeTrackingItem;

import java.io.Closeable;

public interface ItemWriter extends Closeable {
    void write(TimeTrackingItem item);

    void close();
}
