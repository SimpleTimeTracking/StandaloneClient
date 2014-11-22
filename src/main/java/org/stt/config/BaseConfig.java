package org.stt.config;

public class BaseConfig {
	private TimeTrackingItemListConfig timeTrackingItemListConfig = new TimeTrackingItemListConfig();

	public void setTimeTrackingItemListConfig(
			TimeTrackingItemListConfig timeTrackingItemListConfig) {
		this.timeTrackingItemListConfig = timeTrackingItemListConfig;
	}

	public TimeTrackingItemListConfig getTimeTrackingItemListConfig() {
		return timeTrackingItemListConfig;
	}
}
