package org.stt.persistence

import org.stt.model.TimeTrackingItem
import java.io.Closeable
import java.util.*

/**
 *
 *
 * Unless otherwise specified, the items returned **must** be ordered by
 * start time.
 *
 *
 *
 * Note that only the last item returned should have no "end" time, all others
 * should be "closed"
 *
 */
interface ItemReader : Closeable {
    /**
     * Reads an item, if available.
     *
     * @return An [Optional] of the [TimeTrackingItem] or absent if
     * none is available
     */
    fun read(): TimeTrackingItem?
}
