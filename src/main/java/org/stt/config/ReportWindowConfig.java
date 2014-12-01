package org.stt.config;

/**
 * Created by dante on 01.12.14.
 */
public class ReportWindowConfig implements Config {
    private boolean groupItems = true;

    public boolean isGroupItems() {
        return groupItems;
    }

    public void setGroupItems(boolean groupItems) {
        this.groupItems = groupItems;
    }

    @Override
    public void applyDefaults() {

    }
}
