package org.stt.command;

import org.stt.model.TimeTrackingItem;

import java.util.Collection;

public interface CommandHandler {
    void addNewActivity(NewActivity command);

    void endCurrentActivity(EndCurrentItem command);

    void removeActivity(RemoveActivity command);

    void removeActivityAndCloseGap(RemoveActivity command);

    void resumeActivity(ResumeActivity command);

    void resumeLastActivity(ResumeLastActivity command);

    void bulkChangeActivity(Collection<TimeTrackingItem> itemsToChange, String activity);
}
