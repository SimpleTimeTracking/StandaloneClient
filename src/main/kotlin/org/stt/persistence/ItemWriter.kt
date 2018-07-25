package org.stt.persistence

import org.stt.model.TimeTrackingItem

import java.io.Closeable

interface ItemWriter : Closeable {
    fun write(item: TimeTrackingItem)
}
