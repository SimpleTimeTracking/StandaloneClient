package org.stt.command;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

public class ResumeLastActivity implements Command {
    public final LocalDateTime resumeAt;

    public ResumeLastActivity(LocalDateTime resumeAt) {
        this.resumeAt = requireNonNull(resumeAt);
    }

    @Override
    public void accept(CommandHandler commandHandler) {
        commandHandler.resumeLastActivity(this);
    }
}
