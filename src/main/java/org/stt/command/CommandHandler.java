package org.stt.command;

public interface CommandHandler {
    void addNewActivity(NewItemCommand command);

    void endCurrentActivity(EndCurrentItem command);

    void removeActivity(RemoveActivity command);

    void resumeActivity(ResumeActivity command);
}
