package org.stt.persistence.stt

import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.io.UncheckedIOException
import javax.inject.Inject

/**
 * Imports all time tracking records written by [STTItemPersister]
 */
class STTItemReader @Inject
constructor(@STTFile input: Reader) : ItemReader {

    private var reader: BufferedReader? = BufferedReader(input)

    private val converter = STTItemConverter()

    override fun read(): TimeTrackingItem? {
        val source = reader ?: return null
        try {
            var line: String?
            do {
                line = source.readLine()
            } while (line?.isBlank() == true)
            line?.let { return converter.lineToTimeTrackingItem(it) }
            source.close()
            reader = null
            return null
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    override fun close() {
        reader?.close()
        reader = null
    }
}
