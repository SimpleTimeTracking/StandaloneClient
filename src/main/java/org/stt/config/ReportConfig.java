package org.stt.config;

import java.time.Duration;

public class ReportConfig implements ConfigurationContainer {
    private Duration roundDurationsTo = Duration.ofMinutes(5);
    private boolean groupItems = true;

    public boolean isGroupItems() {
        return groupItems;
    }

    public void setGroupItems(boolean groupItems) {
        this.groupItems = groupItems;
    }

    public Duration getRoundDurationsTo() {
        return roundDurationsTo;
    }

    public void setRoundDurationsTo(Duration roundDurationsTo) {
        this.roundDurationsTo = roundDurationsTo;
    }
}
