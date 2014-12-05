package org.stt.fun;

import com.google.common.base.Preconditions;

import java.util.ResourceBundle;

/**
 *
 * @author dante
 */
class LocalizedAchievement extends Achievement {

	private ResourceBundle resourceBundle;

	public LocalizedAchievement(ResourceBundle resourceBundle) {
		this.resourceBundle = Preconditions.checkNotNull(resourceBundle);
	}

	protected String localize(String key) {
		return resourceBundle.getString(key);
	}
}
