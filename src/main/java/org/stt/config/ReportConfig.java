package org.stt.config;

import java.time.Duration;

public class ReportConfig implements ConfigurationContainer {
    private Duration roundDurationsTo = Duration.ofMinutes(5);

    public Duration getRoundDurationsTo() {
        return roundDurationsTo;
    }

    public void setRoundDurationsTo(Duration roundDurationsTo) {
        this.roundDurationsTo = roundDurationsTo;
    }
}
