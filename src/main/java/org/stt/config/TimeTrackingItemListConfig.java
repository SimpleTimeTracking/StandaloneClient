package org.stt.config;

public class TimeTrackingItemListConfig implements Config {
	private boolean filterDuplicatesWhenSearching = false;

	/**
	 * When true, only distinct comments will be shown in the result list.
	 */
	public boolean isFilterDuplicatesWhenSearching() {
		return filterDuplicatesWhenSearching;
	}

	public void setFilterDuplicatesWhenSearching(
			boolean filterDuplicatesWhenSearching) {
		this.filterDuplicatesWhenSearching = filterDuplicatesWhenSearching;
	}

	@Override
	public void applyDefaults() {
	}

}
