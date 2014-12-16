package org.stt.config;

/**
 * Created by dante on 13.12.14.
 */
public class CommandTextConfig implements Config {
    private boolean autoCompletionPopup = false;

    public boolean isAutoCompletionPopup() {
        return autoCompletionPopup;
    }

    public void setAutoCompletionPopup(boolean autoCompletionPopup) {
        this.autoCompletionPopup = autoCompletionPopup;
    }

    @Override
    public void applyDefaults() {

    }
}
