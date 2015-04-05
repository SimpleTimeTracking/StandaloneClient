package org.stt.command;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 14.03.15.
 */
public class EndCurrentItemCommand extends PersistingCommand {
    private TimeTrackingItem itemToReplace;
    public final TimeTrackingItem endedItem;

    EndCurrentItemCommand(ItemPersister persister, TimeTrackingItem unfinishedItem, TimeTrackingItem finishedItem) {
        super(persister);
        itemToReplace = checkNotNull(unfinishedItem);
        this.endedItem = checkNotNull(finishedItem);
    }

    @Override
    public void execute() {
        try {
            persister.replace(itemToReplace, endedItem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
