package org.stt.config;

public class BaseConfig implements Config {
	private TimeTrackingItemListConfig timeTrackingItemListConfig;

	public void setTimeTrackingItemListConfig(
			TimeTrackingItemListConfig timeTrackingItemListConfig) {
		this.timeTrackingItemListConfig = timeTrackingItemListConfig;
	}

	public TimeTrackingItemListConfig getTimeTrackingItemListConfig() {
		return timeTrackingItemListConfig;
	}

	@Override
	public void applyDefaults() {
		if (timeTrackingItemListConfig == null) {
			timeTrackingItemListConfig = new TimeTrackingItemListConfig();
		}
		timeTrackingItemListConfig.applyDefaults();
	}
}
