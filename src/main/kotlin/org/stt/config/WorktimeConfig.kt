package org.stt.config

import java.time.DayOfWeek
import java.time.Duration
import java.util.*

class WorktimeConfig : ConfigurationContainer {
    var breakActivities = Arrays.asList("pause", "break", "coffee")
    var workingHours: MutableMap<String, Duration> = DayOfWeek.values()
            .map {
                it.name to (if (it == DayOfWeek.SUNDAY) Duration.ZERO else Duration.ofHours(8))
            }.toMap().toMutableMap()
    var workingTimesFile = PathSetting("\$HOME$/.stt/worktimes")

}
