package org.stt.config;

public class ActivitiesConfig {
    private boolean filterDuplicatesWhenSearching = false;
    private boolean askBeforeDeleting = false;
    private boolean autoCompletionPopup = false;

    public boolean isAutoCompletionPopup() {
        return autoCompletionPopup;
    }

    public void setAutoCompletionPopup(boolean autoCompletionPopup) {
        this.autoCompletionPopup = autoCompletionPopup;
    }

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
}
