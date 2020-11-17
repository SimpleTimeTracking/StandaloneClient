package org.stt.config

class ActivitiesConfig : ConfigurationContainer {
    /**
     * When true, only distinct comments will be shown in the result list.
     */
    var isFilterDuplicatesWhenSearching = false
    var isAskBeforeDeleting = true
    var isAutoCompletionPopup = false
    var isCloseOnContinue = true
    var isDeleteClosesGaps = true
    var isCloseOnStop = true

    var isGroupItems = true
}
