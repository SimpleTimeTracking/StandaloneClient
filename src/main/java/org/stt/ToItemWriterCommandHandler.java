package org.stt;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.command.*;
import org.stt.event.ShutdownRequest;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.search.ItemSearcher;
import org.stt.time.DateTimeHelper;

import java.io.IOException;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ToItemWriterCommandHandler implements CommandHandler {

    public static final String COMMAND_FIN = "fin";
    private static Logger LOG = Logger
            .getLogger(ToItemWriterCommandHandler.class.getName());
    private final ItemPersister itemWriter;
    private final ItemSearcher itemSearcher;
    private ItemGenerator itemGenerator = new ItemGenerator();
    private CommandParser parser;

    @Inject
    public ToItemWriterCommandHandler(ItemPersister itemWriter,
                                      ItemSearcher itemSearcher) {
        this.itemWriter = checkNotNull(itemWriter);
        this.itemSearcher = checkNotNull(itemSearcher);
        this.parser = new CommandParser(itemWriter, itemSearcher);
    }

    @Override
    public Optional<TimeTrackingItem> executeCommand(String command) {
        checkNotNull(command);
        Command parsedCommand = parser.executeCommand(command).or(NothingCommand.INSTANCE);
        parsedCommand.execute();
        if (parsedCommand instanceof NewItemCommand) {
            return Optional.of(((NewItemCommand) parsedCommand).newItem);
        } else if (parsedCommand instanceof EndCurrentItemCommand) {
            return Optional.of(((EndCurrentItemCommand) parsedCommand).endedItem);
        }
        return Optional.absent();
    }

    @Override
    public void endCurrentItem() {
        endCurrentItem(DateTime.now());
    }

    @Override
    public Optional<TimeTrackingItem> endCurrentItem(DateTime endTime) {
        Command endCurrentItemCommand = parser.endCurrentItem(endTime).or(NothingCommand.INSTANCE);
        endCurrentItemCommand.execute();
        if (endCurrentItemCommand instanceof EndCurrentItemCommand) {
            return Optional.of(((EndCurrentItemCommand) endCurrentItemCommand).endedItem);
        }
        return Optional.absent();
    }

    @Override
    public void resumeGivenItem(TimeTrackingItem item) {
        TimeTrackingItem newItem = new TimeTrackingItem(
                item.getComment().get(), DateTime.now());
        parser.resumeItem(newItem).execute();
    }

    @Override
    public void close() throws IOException {
        itemWriter.close();
    }

    @Subscribe
    public void closeOnShutdown(ShutdownRequest request) {
        try {
            close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(TimeTrackingItem item) throws IOException {
        checkNotNull(item);
        parser.deleteCommandFor(item).execute();
    }

}
