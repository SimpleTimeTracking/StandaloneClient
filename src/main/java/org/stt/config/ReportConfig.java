package org.stt.config;

import java.time.Duration;
import java.util.List;

import static java.util.Arrays.asList;

public class ReportConfig implements ConfigurationContainer {
    private Duration roundDurationsTo = Duration.ofMinutes(5);
    private boolean groupItems = true;
    private List<String> groupColors = asList("BLUE", "DARKCYAN", "GREEN", "DARKGREEN", "BROWN");

    public boolean isGroupItems() {
        return groupItems;
    }

    public void setGroupItems(boolean groupItems) {
        this.groupItems = groupItems;
    }

    public void setGroupColors(List<String> groupColors) {
        this.groupColors = groupColors;
    }

    public List<String> getGroupColors() {
        return groupColors;
    }

    public Duration getRoundDurationsTo() {
        return roundDurationsTo;
    }

    public void setRoundDurationsTo(Duration roundDurationsTo) {
        this.roundDurationsTo = roundDurationsTo;
    }
}
