package org.stt.command

import org.stt.model.TimeTrackingItem

interface CommandHandler {
    fun addNewActivity(command: NewActivity)

    fun endCurrentActivity(command: EndCurrentItem)

    fun removeActivity(command: RemoveActivity)

    fun removeActivityAndCloseGap(command: RemoveActivity)

    fun resumeActivity(command: ResumeActivity)

    fun resumeLastActivity(command: ResumeLastActivity)

    fun bulkChangeActivity(itemsToChange: Collection<TimeTrackingItem>, activity: String)
}
