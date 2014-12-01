package org.stt.config;

import java.util.Collections;
import java.util.List;

/**
 * Created by dante on 01.12.14.
 */
public class CommonPrefixGrouperConfig implements Config {
    private List<String> baseLine;

    public void setBaseLine(List<String> baseLine) {
        this.baseLine = baseLine;
    }

    public List<String> getBaseLine() {
        return baseLine;
    }

    @Override
    public void applyDefaults() {
        if (baseLine == null) {
            baseLine = Collections.emptyList();
        }
    }
}
