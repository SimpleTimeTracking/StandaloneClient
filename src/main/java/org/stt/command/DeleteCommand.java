package org.stt.command;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 15.03.15.
 */
public class DeleteCommand extends PersistingCommand {
    public final TimeTrackingItem itemToDelete;

    public DeleteCommand(ItemPersister persister, TimeTrackingItem item) {
        super(persister);
        this.itemToDelete = checkNotNull(item);
    }

    @Override
    public void execute() {
        try {
            persister.delete(itemToDelete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
