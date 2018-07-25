package org.stt.importer

import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import java.util.*

object IOUtil {

    fun readAll(reader: ItemReader): Collection<TimeTrackingItem> {
        val result = ArrayList<TimeTrackingItem>()
        while (true) {
            reader.read()?.let { result.add(it) } ?: break
        }
        reader.close()
        return result
    }
}
