package org.stt.event;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.stt.Service;
import org.stt.command.CommandFormatter;
import org.stt.model.ItemDeleted;
import org.stt.model.ItemInserted;
import org.stt.model.ItemReplaced;
import org.stt.model.TimeTrackingItem;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.PrintWriter;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

/**
 * Created by dante on 20.03.15.
 */
public class ItemLogService implements Service {
    private PrintWriter out;
    private MBassador eventBus;
    private final CommandFormatter formatter;

    @Inject
    public ItemLogService(@Named("itemLog") PrintWriter out,
                          MBassador<Object> eventBus,
                          CommandFormatter formatter) {
        this.out = requireNonNull(out);
        this.eventBus = requireNonNull(eventBus);
        this.formatter = requireNonNull(formatter);
    }

    @Handler
    public void itemInserted(ItemInserted event) {
        log("inserted", event.newItem);
    }

    private void log(String eventType, TimeTrackingItem item) {
        String command = formatter.asNewItemCommandText(item);
        StringBuilder outputLine = new StringBuilder();
        addCurrentTimeTo(outputLine);
        outputLine.append(", ").append(eventType).append(": ");
        outputLine.append(command);
        out.println(outputLine);
    }

    private void addCurrentTimeTo(StringBuilder outputLine) {
        outputLine.append(DateTimes.DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS.format(LocalDateTime.now()));
    }

    @Handler
    public void itemDeleted(ItemDeleted event) {
        log("deleted", event.deletedItem);
    }

    @Handler
    public void itemReplaced(ItemReplaced event) {
        log("before_update", event.beforeUpdate);
        log("after_update", event.afterUpdate);
    }

    @Override
    public void start() throws Exception {
        eventBus.subscribe(this);
    }

    @Override
    public void stop() {
        out.close();
    }
}
