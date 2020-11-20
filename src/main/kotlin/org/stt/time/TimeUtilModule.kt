package org.stt.time

import dagger.Module
import dagger.Provides
import org.stt.config.ReportConfig
import java.util.logging.Logger

@Module
class TimeUtilModule {
    private val log = Logger.getLogger(TimeUtilModule::class.java.name)

    @Provides
    fun provideDurationRounder(reportConfig: ReportConfig): DurationRounder {
        val rounder = DurationRounder()
        val durationToRoundTo = reportConfig.roundDurationsTo
        rounder.setInterval(durationToRoundTo)
        log.info {
            String.format("Rounding to %s", DateTimes.FORMATTER_PERIOD_H_M_S(durationToRoundTo))
        }
        return rounder
    }

}
