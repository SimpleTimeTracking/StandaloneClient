package org.stt.persistence.stt

import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemWriter
import java.io.PrintWriter
import java.io.Writer
import javax.inject.Inject

class STTItemWriter @Inject
constructor(@STTFile out: Writer) : ItemWriter {
    private val out: PrintWriter = PrintWriter(out)
    private val converter = STTItemConverter()

    override fun write(item: TimeTrackingItem) {
        out.println(converter.timeTrackingItemToLine(item))
    }

    override fun close() {
        out.close()
    }
}
