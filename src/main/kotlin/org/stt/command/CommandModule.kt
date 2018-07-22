package org.stt.command

import dagger.Module
import dagger.Provides
import net.engio.mbassy.bus.MBassador
import org.stt.persistence.ItemPersister
import org.stt.query.TimeTrackingItemQueries
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Named

@Module
class CommandModule {

    @Provides
    fun provideCommandHandler(persister: ItemPersister, queries: TimeTrackingItemQueries, eventBus: Optional<MBassador<Any>>): CommandHandler {
        return Activities(persister, queries, eventBus)
    }

    @Provides
    fun provideCommandTextParser(): CommandTextParser {
        return CommandTextParser(
                dateTimeFormatter(null, FormatStyle.SHORT),
                dateTimeFormatter(null, FormatStyle.MEDIUM),
                dateTimeFormatter(FormatStyle.SHORT, FormatStyle.SHORT),
                dateTimeFormatter(FormatStyle.SHORT, FormatStyle.MEDIUM),
                dateTimeFormatter(FormatStyle.MEDIUM, FormatStyle.SHORT),
                dateTimeFormatter(FormatStyle.MEDIUM, FormatStyle.MEDIUM))
    }

    internal fun dateTimeFormatter(dateFormat: FormatStyle?, timeFormat: FormatStyle): DateTimeFormatter {
        return DateTimeFormatterBuilder()
                .parseLenient().appendLocalized(dateFormat, timeFormat).toFormatter()
    }

    @Provides
    @Named("dateTimeFormatter")
    fun provideDateTimeFormatter(): DateTimeFormatter {
        return DateTimeFormatterBuilder()
                .appendLocalized(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
                .toFormatter()
    }

    @Provides
    @Named("timeFormatter")
    fun provideTimeFormatter(): DateTimeFormatter {
        return DateTimeFormatterBuilder()
                .appendLocalized(null, FormatStyle.MEDIUM)
                .toFormatter()
    }
}
