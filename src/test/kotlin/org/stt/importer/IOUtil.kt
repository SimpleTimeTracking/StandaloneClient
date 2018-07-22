package org.stt.importer

import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import java.io.IOException
import java.util.*

object IOUtil {

    @Throws(IOException::class)
    fun readAll(reader: ItemReader): Collection<TimeTrackingItem> {
        val result = ArrayList<TimeTrackingItem>()
        while (true) {
            reader.read()?.let { result.add(it) } ?: break
        }
        reader.close()
        return result
    }
}
