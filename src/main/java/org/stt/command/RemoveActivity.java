package org.stt.command;

import org.stt.model.TimeTrackingItem;

import java.util.Objects;

public class RemoveActivity implements Command {
    public final TimeTrackingItem itemToDelete;

    public RemoveActivity(TimeTrackingItem item) {
        this.itemToDelete = Objects.requireNonNull(item);
    }

    @Override
    public void accept(CommandHandler commandHandler) {
        commandHandler.removeActivity(this);
    }
}
