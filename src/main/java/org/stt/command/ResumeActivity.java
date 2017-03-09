package org.stt.command;

import org.stt.model.TimeTrackingItem;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

public class ResumeActivity implements Command {
    public final TimeTrackingItem itemToResume;
    public final LocalDateTime beginningWith;

    public ResumeActivity(TimeTrackingItem item, LocalDateTime beginningWith) {
        this.itemToResume = requireNonNull(item);
        this.beginningWith = requireNonNull(beginningWith);
    }

    @Override
    public void accept(CommandHandler commandHandler) {
        commandHandler.resumeActivity(this);
    }
}
