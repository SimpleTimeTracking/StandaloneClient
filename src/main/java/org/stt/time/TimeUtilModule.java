package org.stt.time;

import dagger.Module;
import dagger.Provides;
import org.stt.config.ReportConfig;

import java.time.Duration;
import java.util.logging.Logger;

@Module
public class TimeUtilModule {
    private static final Logger LOG = Logger.getLogger(TimeUtilModule.class.getName());

    private TimeUtilModule() {
    }

    @Provides
    static DurationRounder provideDurationRounder(ReportConfig reportConfig) {
        DurationRounder rounder = new DurationRounder();
        final Duration durationToRoundTo = reportConfig.getRoundDurationsTo();
        rounder.setInterval(durationToRoundTo);
        LOG.info(() -> String.format("Rounding to %s", DateTimes.FORMATTER_PERIOD_H_M_S
                .print(durationToRoundTo)));
        return rounder;
    }
    
}
