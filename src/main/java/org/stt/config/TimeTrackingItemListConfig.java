package org.stt.config;

public class TimeTrackingItemListConfig implements Config {
	private boolean filterDuplicatesWhenSearching = false;
    private boolean askBeforeDeleting = false;

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

    public boolean isAskBeforeDeleting() {
        return askBeforeDeleting;
    }

    public void setAskBeforeDeleting(boolean askBeforeDeleting) {
        this.askBeforeDeleting = askBeforeDeleting;
    }

    @Override
    public void applyDefaults() {
    }
}
