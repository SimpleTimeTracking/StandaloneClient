package org.stt.command;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
* Created by dante on 15.03.15.
*/
public class ResumeCommand extends PersistingCommand {
    private final TimeTrackingItem resumedItem;

    public ResumeCommand(ItemPersister persister, TimeTrackingItem item) {
        super(persister);
        this.resumedItem = checkNotNull(item);
    }

    @Override
    public void execute() {
        try {
            persister.insert(resumedItem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
