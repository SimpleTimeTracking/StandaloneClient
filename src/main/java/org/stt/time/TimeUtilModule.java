package org.stt.time;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.joda.time.Duration;
import org.stt.Configuration;

import java.util.logging.Logger;

/**
 * Created by dante on 05.12.14.
 */
public class TimeUtilModule extends AbstractModule {
    private static final Logger LOG = Logger.getLogger(TimeUtilModule.class.getName());

    @Override
    protected void configure() {
        
    }

    @Provides
    DurationRounder provideDurationRounder(Configuration configuration) {
        DurationRounder rounder = new DurationRounder();
        final Duration durationToRoundTo = configuration
                .getDurationToRoundTo();
        rounder.setInterval(durationToRoundTo);
        LOG.info("Rounding to "
                + DateTimeHelper.FORMATTER_PERIOD_H_M_S
                .print(durationToRoundTo.toPeriod()));
        return rounder;
    }
    
}
