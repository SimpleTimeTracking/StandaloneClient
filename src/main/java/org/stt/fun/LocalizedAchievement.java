package org.stt.fun;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 *
 * @author dante
 */
class LocalizedAchievement extends Achievement {

	private ResourceBundle resourceBundle;

	public LocalizedAchievement(ResourceBundle resourceBundle) {
        this.resourceBundle = Objects.requireNonNull(resourceBundle);
    }

	protected String localize(String key) {
		return resourceBundle.getString(key);
	}
}
