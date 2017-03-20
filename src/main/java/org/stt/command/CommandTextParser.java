package org.stt.command;

import org.antlr.v4.runtime.tree.RuleNode;
import org.stt.g4.EnglishCommandsBaseVisitor;
import org.stt.g4.EnglishCommandsParser;
import org.stt.g4.EnglishCommandsVisitor;
import org.stt.model.TimeTrackingItem;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.stt.g4.EnglishCommandsParser.CommandContext;

class CommandTextParser {
    private EnglishCommandsVisitor<Object> parserVisitor = new MyEnglishCommandsBaseVisitor();

    public Object walk(CommandContext commandContext) {
        return commandContext.accept(parserVisitor);
    }

    private static class MyEnglishCommandsBaseVisitor extends EnglishCommandsBaseVisitor<Object> {
        @Override
        public LocalDate visitDate(EnglishCommandsParser.DateContext ctx) {
            return LocalDate.of(ctx.year, ctx.month, ctx.day);
        }

        @Override
        public LocalDateTime visitDateTime(EnglishCommandsParser.DateTimeContext ctx) {
            LocalDate date = ctx.date() != null ? visitDate(ctx.date()) : LocalDate.now();
            return date.atStartOfDay().withHour(ctx.hour).withMinute(ctx.minute).withSecond(ctx.second);
        }

        @Override
        public LocalDateTime[] visitSinceFormat(EnglishCommandsParser.SinceFormatContext ctx) {
            return new LocalDateTime[]{visitDateTime(ctx.start), ctx.end != null ? visitDateTime(ctx.end) : null};
        }

        @Override
        public Object visitResumeLastCommand(EnglishCommandsParser.ResumeLastCommandContext ctx) {
            return new ResumeLastActivity(LocalDateTime.now());
        }

        @Override
        public LocalDateTime[] visitAgoFormat(EnglishCommandsParser.AgoFormatContext ctx) {
            Duration duration;
            int amount = ctx.amount;
            EnglishCommandsParser.TimeUnitContext timeUnit = ctx.timeUnit();
            if (timeUnit.HOURS() != null) {
                duration = Duration.ofHours(amount);
            } else if (timeUnit.MINUTES() != null) {
                duration = Duration.ofMinutes(amount);
            } else if (timeUnit.SECONDS() != null) {
                duration = Duration.ofSeconds(amount);
            } else {
                throw new IllegalStateException("Unknown ago unit: " + ctx.getText());
            }
            return new LocalDateTime[]{LocalDateTime.now().minus(duration), null};
        }

        @Override
        public LocalDateTime[] visitFromToFormat(EnglishCommandsParser.FromToFormatContext ctx) {
            LocalDateTime start = visitDateTime(ctx.start);
            LocalDateTime end = ctx.end != null ? visitDateTime(ctx.end) : null;
            return new LocalDateTime[]{start, end};
        }

        @Override
        public LocalDateTime[] visitTimeFormat(EnglishCommandsParser.TimeFormatContext ctx) {
            LocalDateTime[] result = (LocalDateTime[]) super.visitTimeFormat(ctx);
            if (result == null) {
                return new LocalDateTime[]{LocalDateTime.now(), null};
            }
            return result;
        }

        @Override
        public Object visitItemWithComment(EnglishCommandsParser.ItemWithCommentContext ctx) {
            LocalDateTime[] period = visitTimeFormat(ctx.timeFormat());
            if (period[1] != null) {
                return new TimeTrackingItem(ctx.text, period[0], period[1]);
            } else {
                return new TimeTrackingItem(ctx.text, period[0]);
            }
        }

        @Override
        public LocalDateTime visitFinCommand(EnglishCommandsParser.FinCommandContext ctx) {
            if (ctx.at != null) {
                return visitDateTime(ctx.at);
            }
            return LocalDateTime.now();
        }

        @Override
        protected boolean shouldVisitNextChild(RuleNode node, Object currentResult) {
            return currentResult == null;
        }
    }
}
