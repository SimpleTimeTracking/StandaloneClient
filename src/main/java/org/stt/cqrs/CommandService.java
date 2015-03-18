package org.stt.cqrs;

import com.google.common.eventbus.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 16.03.15.
 */
public class CommandService {
    private final EventBus commandBus;

    public CommandService(EventBus commandBus) {
        this.commandBus = checkNotNull(commandBus);
    }

    public void execute(Command command) {
        commandBus.post(command);
    }
}
