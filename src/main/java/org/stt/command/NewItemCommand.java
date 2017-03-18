package org.stt.command;

import org.stt.model.TimeTrackingItem;

import java.util.Objects;

/**
 * Created by dante on 14.03.15.
 */
public class NewItemCommand implements Command {
    public final TimeTrackingItem newItem;

    NewItemCommand(TimeTrackingItem newItem) {
        this.newItem = Objects.requireNonNull(newItem);
    }

    @Override
    public void accept(CommandHandler commandHandler) {
        commandHandler.addNewActivity(this);
    }
}
