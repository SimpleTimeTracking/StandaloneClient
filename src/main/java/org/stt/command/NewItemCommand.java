package org.stt.command;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 14.03.15.
 */
public class NewItemCommand extends PersistingCommand {
    public final TimeTrackingItem newItem;

    NewItemCommand(ItemPersister persister, TimeTrackingItem newItem) {
        super(persister);
        this.newItem = checkNotNull(newItem);
    }

    @Override
    public void execute() {
        try {
            persister.insert(newItem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
