package org.stt.command;

public class DoNothing implements Command {
    public static final Command INSTANCE = new DoNothing();

    private DoNothing() {
    }

    @Override
    public void accept(CommandHandler commandHandler) {
        // DO NOTHING
    }
}
