package org.stt.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.stt.Service;
import org.stt.command.CommandParser;
import org.stt.event.events.ItemDeletedEvent;
import org.stt.event.events.ItemInsertedEvent;
import org.stt.event.events.ItemReplacedEvent;
import org.stt.model.TimeTrackingItem;
import org.stt.time.DateTimeHelper;

import java.io.PrintWriter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 20.03.15.
 */
public class ItemLogService implements Service {
    private PrintWriter out;
    private EventBus eventBus;

    @Inject
    public ItemLogService(@Named("itemLog") PrintWriter out, EventBus eventBus) {
        this.out = checkNotNull(out);
        this.eventBus = checkNotNull(eventBus);
    }

    @Subscribe
    public void itemInserted(ItemInsertedEvent event) {
        log("inserted", event.newItem);
    }

    private void log(String eventType, TimeTrackingItem item) {
        String command = CommandParser.itemToCommandWithFullDates(item);
        StringBuilder outputLine = new StringBuilder();
        addCurrentTimeTo(outputLine);
        outputLine.append(", " + eventType + ": ");
        outputLine.append(command);
        out.println(outputLine);
    }

    private void addCurrentTimeTo(StringBuilder outputLine) {
        outputLine.append(DateTimeHelper.DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS.print(new DateTime()));
    }

    @Subscribe
    public void itemDeleted(ItemDeletedEvent event) {
        log("deleted", event.deletedItem);
    }

    @Subscribe
    public void itemReplaced(ItemReplacedEvent event) {
        log("before_update", event.beforeUpdate);
        log("after_update", event.afterUpdate);
    }

    @Override
    public void start() throws Exception {
        eventBus.register(this);
    }

    @Override
    public void stop() {
        out.close();
    }
}
