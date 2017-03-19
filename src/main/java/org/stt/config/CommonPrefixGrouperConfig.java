package org.stt.config;

import java.util.Collections;
import java.util.List;

public class CommonPrefixGrouperConfig implements ConfigurationContainer {
    private List<String> baseLine = Collections.emptyList();

    public void setBaseLine(List<String> baseLine) {
        this.baseLine = baseLine;
    }

    public List<String> getBaseLine() {
        return baseLine;
    }
}
