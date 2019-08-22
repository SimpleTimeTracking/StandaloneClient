package org.stt.command

import org.antlr.v4.runtime.CommonTokenStream
import org.stt.grammar.EnglishCommandsLexer
import org.stt.grammar.EnglishCommandsParser
import org.stt.model.TimeTrackingItem
import org.stt.time.DateTimes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named

class CommandFormatter @Inject
constructor(private val commandTextParser: CommandTextParser,
            @param:Named("dateTimeFormatter") private val dateTimeFormatter: DateTimeFormatter,
            @param:Named("timeFormatter") private val timeFormatter: DateTimeFormatter) {

    fun parse(command: String): Command {
        val inputStream = CaseInsensitiveInputStream(command)
        val lexer = EnglishCommandsLexer(inputStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = EnglishCommandsParser(tokenStream)
        val result = commandTextParser.walk(parser.command())
        if (result is TimeTrackingItem) {
            return NewActivity(result)
        }
        return if (result is LocalDateTime) {
            EndCurrentItem(result)
        } else result as? Command ?: DoNothing
    }

    fun asNewItemCommandText(item: TimeTrackingItem): String {
        val start = if (DateTimes.isToday(item.start)) {
            timeFormatter.format(item.start.toLocalTime())
        } else {
            dateTimeFormatter.format(item.start)
        }
        return item.end?.let { endDateTime ->
            val end = if (DateTimes.isToday(endDateTime)) {
                timeFormatter.format(endDateTime.toLocalTime())
            } else {
                dateTimeFormatter.format(endDateTime)
            }
            String.format("%s from %s to %s", item.activity, start, end)
        } ?: String.format("%s since %s", item.activity, start)
    }

}
