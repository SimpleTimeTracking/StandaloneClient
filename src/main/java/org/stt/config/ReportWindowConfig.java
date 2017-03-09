package org.stt.config;

import java.util.List;

import static java.util.Arrays.asList;

public class ReportWindowConfig {
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
}
