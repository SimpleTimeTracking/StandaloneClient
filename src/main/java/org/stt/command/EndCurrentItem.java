package org.stt.command;

import java.time.LocalDateTime;
import java.util.Objects;

public class EndCurrentItem implements Command {
    public final LocalDateTime endAt;

    public EndCurrentItem(LocalDateTime endAt) {
        Objects.requireNonNull(endAt);
        this.endAt = endAt;
    }

    @Override
    public void accept(CommandHandler commandHandler) {
        commandHandler.endCurrentActivity(this);
    }
}
