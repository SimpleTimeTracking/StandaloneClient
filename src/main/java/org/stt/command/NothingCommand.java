package org.stt.command;

/**
 * Created by dante on 14.03.15.
 */
public class NothingCommand implements Command {
    public static final Command INSTANCE = new NothingCommand();

    private NothingCommand() {}

    @Override
    public void execute() {

    }
}
