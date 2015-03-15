package org.stt.command;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

/**
 * Created by dante on 14.03.15.
 */
public interface Command {
    void execute();
}
