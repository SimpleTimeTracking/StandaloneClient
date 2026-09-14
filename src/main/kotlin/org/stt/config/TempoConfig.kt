package org.stt.config

import java.time.Duration

enum class TempoSummaryMode {
    ITEMS,
    GROUPED
}

class TempoConfig : ConfigurationContainer {
    var tempoToken: PasswordSetting? = null
    var workdayStart: Duration = Duration.ofHours(8)
    var summaryMode = TempoSummaryMode.ITEMS
}
