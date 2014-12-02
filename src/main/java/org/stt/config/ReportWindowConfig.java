package org.stt.config;

import javafx.scene.paint.Color;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by dante on 01.12.14.
 */
public class ReportWindowConfig implements Config {
    private boolean groupItems = true;
    private List<String> groupColors;

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

    @Override
    public void applyDefaults() {
        if (groupColors == null)  {
            groupColors = asList(new String[]{"BLUE", "DARKCYAN", "GREEN", "DARKGREEN", "BROWN"});
        }
    }
}
