package org.stt.command;

public interface CommandHandler {
    void addNewActivity(NewActivity command);

    void endCurrentActivity(EndCurrentItem command);

    void removeActivity(RemoveActivity command);

    void removeActivityAndFillGap(RemoveActivity command);

    void resumeActivity(ResumeActivity command);

    void resumeLastActivity(ResumeLastActivity command);
}
