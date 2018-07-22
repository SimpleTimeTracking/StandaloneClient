package org.stt.reporting

import org.stt.config.WorktimeConfig
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem
import org.stt.time.DateTimes
import java.io.*
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDate
import java.util.*
import java.util.Objects.requireNonNull
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named

/**
 * Reads information about working times from the configured workingTimes file
 * and aggregates them into [WorkingtimeItem]s
 */
class WorkingtimeItemProvider @Inject
constructor(config: WorktimeConfig,
            @Named("homePath") homePath: String) {
    private val config: WorktimeConfig
    private val workingHoursPerDay = HashMap<LocalDate, WorkingtimeItem>()

    /**
     *
     * @return the dates and corresponding absence times
     */
    // if time is negative...
    val overtimeAbsences: Map<LocalDate, WorkingtimeItem>
        get() {
            val overtimeAbsenceDuration = TreeMap<LocalDate, WorkingtimeItem>()
            for ((key, value) in workingHoursPerDay) {
                if (value.min.isNegative) {
                    overtimeAbsenceDuration[key] = value
                }
            }

            return overtimeAbsenceDuration
        }

    init {
        this.config = requireNonNull(config)

        val workingTimesFile = config.workingTimesFile.file(homePath)
        if (workingTimesFile.exists()) {
            populateHoursMapsFromFile(workingTimesFile)
        }
    }

    /**
     *
     * @return the configured duration to be worked without producing positive
     * or negative overtime
     */
    fun getWorkingTimeFor(date: LocalDate): WorkingtimeItem {
        return workingHoursPerDay[date]
                ?: return fromDuration(config.workingHours.getOrDefault(date.dayOfWeek.name, Duration.ZERO))
    }

    private fun populateHoursMapsFromFile(workingTimesFile: File) {
        BufferedReader(InputStreamReader(constructReaderFrom(workingTimesFile), StandardCharsets.UTF_8))
                .forEachLine { currentLine ->
                    if (currentLine.matches("^\\d+.*".toRegex())) {
                        // it is a date
                        val split = currentLine.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val parseDateTime = LocalDate.parse(split[0], DateTimes.DATE_TIME_FORMATTER_YYYY_MM_DD_DASHED)
                        val minHours = split[1]
                        var maxHours = minHours

                        if (split.size > 2) {
                            maxHours = split[2]
                        }
                        workingHoursPerDay[parseDateTime] = fromHours(minHours, maxHours)

                    } else if (currentLine.startsWith("hours")) {
                        LOG.severe("'hours' is no longer supported in your worktimes file, please setup default working hours in your configuration.")
                    }
                }
    }

    @Throws(FileNotFoundException::class)
    private fun constructReaderFrom(workingTimesFile: File): InputStream {
        return if (workingTimesFile.name.equals("-", ignoreCase = true)) {
            System.`in`
        } else FileInputStream(workingTimesFile)
    }

    private fun fromHours(minHours: String, maxHours: String): WorkingtimeItem {
        val minDur = Duration.ofHours(java.lang.Long.parseLong(minHours))
        val maxDur = Duration.ofHours(java.lang.Long.parseLong(maxHours))
        return WorkingtimeItem(minDur, maxDur)
    }

    private fun fromDuration(duration: Duration): WorkingtimeItem {
        return WorkingtimeItem(duration, duration)
    }

    /**
     *
     */
    data class WorkingtimeItem(val min: Duration, val max: Duration)

    companion object {
        private val LOG = Logger.getLogger(WorkingtimeItemProvider::class.java.simpleName)
    }
}
