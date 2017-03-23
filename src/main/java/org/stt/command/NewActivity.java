package org.stt.command;

import org.stt.model.TimeTrackingItem;

import java.util.Objects;

public class NewActivity implements Command {
    public final TimeTrackingItem newItem;

    public NewActivity(TimeTrackingItem newItem) {
        this.newItem = Objects.requireNonNull(newItem);
    }

    @Override
    public void accept(CommandHandler commandHandler) {
        commandHandler.addNewActivity(this);
    }
}
