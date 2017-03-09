package org.stt.time;

import dagger.Module;
import dagger.Provides;
import org.stt.Configuration;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * Created by dante on 05.12.14.
 */
@Module
public class TimeUtilModule {
    private static final Logger LOG = Logger.getLogger(TimeUtilModule.class.getName());

    private TimeUtilModule() {
    }

    @Provides
    static DurationRounder provideDurationRounder(Configuration configuration) {
        DurationRounder rounder = new DurationRounder();
        final Duration durationToRoundTo = configuration
                .getDurationToRoundTo();
        rounder.setInterval(durationToRoundTo);
        LOG.info(() -> "Rounding to "
                + DateTimes.FORMATTER_PERIOD_H_M_S
                .print(durationToRoundTo));
        return rounder;
    }
    
}
