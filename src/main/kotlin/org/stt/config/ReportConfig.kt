package org.stt.config

import java.time.Duration

class ReportConfig : ConfigurationContainer {
    var roundDurationsTo = Duration.ofMinutes(5)
}
