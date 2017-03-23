package org.stt.config;

public class ActivitiesConfig implements ConfigurationContainer {
    private boolean filterDuplicatesWhenSearching = false;
    private boolean askBeforeDeleting = true;
    private boolean autoCompletionPopup = false;
    private boolean closeOnContinue = true;
    private boolean deleteClosesGaps = true;

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

    public boolean isCloseOnContinue() {
        return closeOnContinue;
    }

    public void setCloseOnContinue(boolean closeOnContinue) {
        this.closeOnContinue = closeOnContinue;
    }

    public boolean isDeleteClosesGaps() {
        return deleteClosesGaps;
    }

    public void setDeleteClosesGaps(boolean deleteClosesGaps) {
        this.deleteClosesGaps = deleteClosesGaps;
    }
}
