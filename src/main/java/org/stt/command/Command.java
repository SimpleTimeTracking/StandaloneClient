package org.stt.command;

public interface Command {
    void accept(CommandHandler commandHandler);
}
