package org.stt.csv.importer

import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import org.stt.time.DateTimes
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.io.UncheckedIOException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Imports from .csv files
 */
class CsvImporter(input: Reader, private val datefieldIndex: Int, private val timefieldIndex: Int,
                  private val commentfieldIndex: Int) : ItemReader {

    private val reader: BufferedReader

    private val formatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy")
    private val durationParser = DateTimeFormatter
            .ofPattern("HH:mm")

    private var nextStartTime: LocalDateTime? = null

    init {
        reader = BufferedReader(input)
    }

    override fun read(): TimeTrackingItem? {
        try {
            var line: String?
            do {
                line = reader.readLine()
            } while (line?.isBlank() == true)
            line?.let { return constructFrom(it, nextStartTime) }
            reader.close()
            return null
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    fun constructFrom(line: String, startTime: LocalDateTime?): TimeTrackingItem? {

        // we want all items, even empty ones: negative parameter to split does
        // exactly that
        val split = line.split(";".toRegex()).toTypedArray()
        if (split.size > Math.max(
                        Math.max(commentfieldIndex, datefieldIndex), timefieldIndex)) {
            val dateString = split[datefieldIndex].replace("\"".toRegex(), "")
            val durationString = split[timefieldIndex].replace("\"".toRegex(), "")
            val comment = split[commentfieldIndex]
            try {
                val parsedDate = LocalDate.parse(dateString, formatter)
                val parsedDateTime = LocalDateTime.of(parsedDate, LocalTime.MIDNIGHT)
                val period = Duration.between(LocalTime.MIDNIGHT, LocalTime.parse(durationString, durationParser))
                var itemStartTime = startTime
                if (!DateTimes.isOnSameDay(startTime, parsedDateTime)) {
                    itemStartTime = parsedDateTime.toLocalDate().atStartOfDay()
                }
                val itemEndTime = itemStartTime!!.plus(period)

                return TimeTrackingItem(comment,
                        itemStartTime, itemEndTime)
            } catch (i: DateTimeParseException) {
                LOG.log(Level.INFO, "not parseable line: $line", i)
            }

        } else {
            LOG.info { "not parseable line: $line" }
        }
        return null
    }

    override fun close() {
        try {
            reader.close()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    companion object {

        private val LOG = Logger.getLogger(CsvImporter::class.java
                .name)
    }
}
