package org.stt.cli

import org.antlr.v4.runtime.CommonTokenStream
import org.stt.command.CaseInsensitiveInputStream
import org.stt.config.CliConfig
import org.stt.grammar.EnglishCommandsLexer
import org.stt.grammar.EnglishCommandsParser
import org.stt.model.TimeTrackingItem
import org.stt.query.Criteria
import org.stt.query.TimeTrackingItemQueries
import org.stt.reporting.OvertimeReportGenerator
import org.stt.reporting.SummingReportGenerator
import org.stt.reporting.WorkingtimeItemProvider
import org.stt.text.ItemCategorizer
import org.stt.text.ItemCategorizer.ItemCategory
import org.stt.time.DateTimes
import org.stt.time.until
import java.io.PrintStream
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Prints a nicely formatted report of [TimeTrackingItem]s
 */
class ReportPrinter @Inject
constructor(private val queries: TimeTrackingItemQueries,
            private val configuration: CliConfig,
            private val workingtimeItemProvider: WorkingtimeItemProvider,
            private val categorizer: ItemCategorizer) {

    fun report(args: MutableCollection<String>, printTo: PrintStream) {
        var searchString: String? = null
        var reportStart = LocalDate.now()
        var reportEnd = reportStart.plusDays(1)
        var truncateLongLines = true

        if (!args.isEmpty()) {
            // there is a parameter! Let's parse it ;-)

            truncateLongLines = !args.remove("long")

            // first collapse all following strings
            val argsString = args.joinToString(" ")

            val lexer = EnglishCommandsLexer(
                    CaseInsensitiveInputStream(argsString))
            val parser = EnglishCommandsParser(
                    CommonTokenStream(lexer))

            val startContext = parser.reportStart()
            if (startContext.from_date != null) {
                reportStart = startContext.from_date
                reportEnd = startContext.to_date.plusDays(1)
            } else if (!argsString.isEmpty()) {
                searchString = argsString
                reportStart = LocalDate.MIN
            }
        }

        var output = ("output " + (if (truncateLongLines) "truncated" else "full")
                + " lines for ")
        output +=
                if (DateTimes.isToday(reportStart)) "today " else {
                    (DateTimes.prettyPrintDate(reportStart) + " to "
                            + DateTimes.prettyPrintDate(reportEnd))
                }
        printTo.println(output)

        printDetails(printTo, searchString, reportStart, reportEnd,
                truncateLongLines)

        printSums(printTo, searchString, reportStart, reportEnd,
                truncateLongLines)

        // only print overtime if we don't search for specific items
        // In this case overtime is just confusing
        if (searchString == null || searchString.isEmpty()) {
            printOvertime(printTo, reportStart, reportEnd)
        }
    }

    private fun printOvertime(printTo: PrintStream, reportStart: LocalDate,
                              reportEnd: LocalDate) {
        val overtimeReportGenerator = createOvertimeReportGenerator()
        val overtimeMap = overtimeReportGenerator
                .getOvertime(reportStart, reportEnd)
        val overallOvertime = overtimeReportGenerator.overallOvertime

        if (DateTimes.isToday(reportStart)) {
            printTo.println("====== times for today: ======")
            val duration = overtimeMap[LocalDate.now()]
            if (duration != null) {
                val closingTime = DateTimes.prettyPrintTime(LocalDateTime
                        .now().minus(duration))
                printTo.println("closing time: $closingTime")
                val timeToGo = DateTimes
                        .prettyPrintDuration(duration.negated())
                printTo.println("time to go:   $timeToGo")
            }

        } else {
            printTo.println("====== overtime from "
                    + DateTimes.prettyPrintDate(reportStart) + " to "
                    + DateTimes.prettyPrintDate(reportEnd) + ": ======")
            var overallDuration = Duration.ZERO
            for ((key, value) in overtimeMap) {
                overallDuration = overallDuration.plus(value)

                printTo.println(DateTimes.prettyPrintDate(key)
                        + " "
                        + DateTimes.prettyPrintDuration(value)
                        + " overall: "
                        + DateTimes.prettyPrintDuration(overallDuration))
            }
            printTo.print("sum:       ")
            printTo.println(DateTimes.prettyPrintDuration(overallDuration))
        }
        printTo.println("overall overtime: " + DateTimes.prettyPrintDuration(overallOvertime))

    }

    /**
     * Prints a nice summed and grouped (by comment) report
     */
    private fun printSums(printTo: PrintStream, searchString: String?,
                          reportStart: LocalDate?, reportEnd: LocalDate, truncateLongLines: Boolean) {
        val criteria = Criteria()
        if (searchString != null) {
            criteria.withActivityContains(searchString)
        }
        criteria.withStartBetween(reportStart!! until reportEnd)

        queries.queryItems(criteria).use { itemsToConsider ->
            val reporter = SummingReportGenerator(itemsToConsider)
            val report = reporter.createReport()

            if (DateTimes.isToday(reportStart)) {
                printTo.println("====== sums of today ======")
                report.start?.let { printTo.println("start of day: " + DateTimes.prettyPrintTime(it)) }
                report.end?.let { printTo.println("end of day:   " + DateTimes.prettyPrintTime(report.end)) }
            } else {
                printTo.println("====== sums from "
                        + DateTimes.prettyPrintDate(reportStart) + " to "
                        + DateTimes.prettyPrintDate(reportEnd))
            }
            if (report.uncoveredDuration != Duration.ZERO) {
                printTo.println("time not yet tracked: " + DateTimes.prettyPrintDuration(report
                        .uncoveredDuration))
            }
            val reportingItems = report.reportingItems

            var worktimeDuration = Duration.ZERO
            var breakTimeDuration = Duration.ZERO
            for ((duration, comment) in reportingItems) {
                var prefix = " "
                if (ItemCategory.BREAK == categorizer.getCategory(comment)) {
                    prefix = "*"
                    breakTimeDuration = breakTimeDuration.plus(duration)
                } else {
                    worktimeDuration = worktimeDuration.plus(duration)
                }
                printTruncatedString(
                        prefix + DateTimes.prettyPrintDuration(duration)
                                + "   " + comment, printTo, truncateLongLines)
            }

            printTo.println("====== overall sum: ======")
            printTo.println("work:  " + DateTimes.prettyPrintDuration(worktimeDuration))
            printTo.println("break: " + DateTimes.prettyPrintDuration(breakTimeDuration))
        }
    }

    /**
     * Prints all items nicely formatted
     */
    private fun printDetails(printTo: PrintStream, searchString: String?,
                             reportStart: LocalDate?, reportEnd: LocalDate, truncateLongLines: Boolean) {

        printTo.println("====== recorded items: ======")

        val criteria = Criteria()
                .withStartBetween(reportStart!! until reportEnd)
        queries.queryItems(criteria).use { itemStream ->
            itemStream.forEach { (comment, start, end) ->
                val builder = StringBuilder()
                builder.append(DateTimes.prettyPrintTime(start))
                builder.append(" - ")
                if (end == null) {
                    builder.append("now     ")
                } else {
                    builder.append(DateTimes.prettyPrintTime(end))
                }
                builder.append(" ( ")
                builder.append(DateTimes.prettyPrintDuration(Duration.between(
                        start, end ?: LocalDateTime.now())))
                builder.append(" ) ")
                builder.append(" => ")
                builder.append(comment)
                if (searchString == null || builder.toString().contains(searchString)) {
                    printTruncatedString(builder, printTo, truncateLongLines)
                }
            }
        }
    }

    private fun createOvertimeReportGenerator(): OvertimeReportGenerator {
        return OvertimeReportGenerator(queries, categorizer,
                workingtimeItemProvider)
    }

    private fun printTruncatedString(toPrint: StringBuilder,
                                     printTo: PrintStream, doTruncate: Boolean) {
        printTruncatedString(toPrint.toString(), printTo, doTruncate)
    }

    private fun printTruncatedString(toPrint: String, printTo: PrintStream,
                                     doTruncate: Boolean) {

        val desiredWidth = Math.max(configuration.cliReportingWidth - 3,
                10)
        if (doTruncate && desiredWidth < toPrint.length) {
            val substr = toPrint.substring(0, desiredWidth)
            printTo.println("$substr...")
        } else {
            printTo.println(toPrint)
        }
    }
}
