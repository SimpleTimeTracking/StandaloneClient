package org.stt.ti.importer

import org.stt.States.requireThat
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.io.UncheckedIOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Imports all time tracking records of an existing (modified) ti installation.
 * All elements of the given inputFile are read. Format of the file has to be
 * "$comment $start to $end" where $comment, $start, and $end do not contain
 * white space
 */
class TiImporter(input: Reader) : ItemReader {

    private val reader: BufferedReader = BufferedReader(input)
    private val dateFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd_HH:mm:ss")

    override fun read(): TimeTrackingItem? {
        try {
            var line: String?
            do {
                line = reader.readLine()
            } while (line?.isBlank() == true)
            line?.let { return constructFrom(it) }
            reader.close()
            return null
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    private fun constructFrom(singleLine: String): TimeTrackingItem {

        val splitLine = singleLine.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        requireThat(
                splitLine.size == 4 || splitLine.size == 2,
                "The given line \""
                        + singleLine
                        + "\" must contain exactly 2 or 4 white space separated elements.")

        var comment = splitLine[0]
        comment = comment.replace("_".toRegex(), " ")

        val start = LocalDateTime.parse(splitLine[1], dateFormat)
        if (splitLine.size > 2) {
            val end = LocalDateTime.parse(splitLine[3], dateFormat)

            return TimeTrackingItem(comment, start, end)
        }

        return TimeTrackingItem(comment, start)
    }

    override fun close() {
        try {
            reader.close()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

}
