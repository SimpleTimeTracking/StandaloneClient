package org.stt.cli;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.stt.config.CliConfig;
import org.stt.grammar.EnglishCommandsLexer;
import org.stt.grammar.EnglishCommandsParser;
import org.stt.grammar.EnglishCommandsParser.ReportStartContext;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.query.Criteria;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.OvertimeReportGenerator;
import org.stt.reporting.SummingReportGenerator;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.reporting.WorkingtimeItemProvider;
import org.stt.text.ItemCategorizer;
import org.stt.text.ItemCategorizer.ItemCategory;
import org.stt.time.DateTimes;
import org.stt.time.Interval;

import javax.inject.Inject;
import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Prints a nicely formatted report of {@link TimeTrackingItem}s
 */
public class ReportPrinter {

    private final TimeTrackingItemQueries queries;
    private final CliConfig configuration;
    private final WorkingtimeItemProvider workingtimeItemProvider;
    private final ItemCategorizer categorizer;

    @Inject
    public ReportPrinter(TimeTrackingItemQueries queries,
                         CliConfig configuration,
                         WorkingtimeItemProvider workingtimeItemProvider,
                         ItemCategorizer categorizer) {
        this.queries = queries;
        this.configuration = configuration;
        this.workingtimeItemProvider = workingtimeItemProvider;
        this.categorizer = categorizer;
    }

    public void report(Collection<String> args, PrintStream printTo) {
        String searchString = null;
        LocalDate reportStart = LocalDate.now();
        LocalDate reportEnd = reportStart.plusDays(1);
        boolean truncateLongLines = true;

        if (!args.isEmpty()) {
            // there is a parameter! Let's parse it ;-)

            truncateLongLines = !args.remove("long");

            // first collapse all following strings
            String argsString = String.join(" ", args);

            EnglishCommandsLexer lexer = new EnglishCommandsLexer(
                    new ANTLRInputStream(argsString));
            EnglishCommandsParser parser = new EnglishCommandsParser(
                    new CommonTokenStream(lexer));

            ReportStartContext startContext = parser.reportStart();
            if (startContext.from_date != null) {
                reportStart = startContext.from_date;
                reportEnd = startContext.to_date.plusDays(1);
            } else if (!argsString.isEmpty()) {
                searchString = argsString;
                reportStart = LocalDate.MIN;
            }
        }

        String output = "output " + (truncateLongLines ? "truncated" : "full")
                + " lines for ";
        if (DateTimes.isToday(reportStart)) {
            output += "today ";

        } else {
            output += DateTimes.prettyPrintDate(reportStart) + " to "
                    + DateTimes.prettyPrintDate(reportEnd);
        }
        printTo.println(output);

        printDetails(printTo, searchString, reportStart, reportEnd,
                truncateLongLines);

        printSums(printTo, searchString, reportStart, reportEnd,
                truncateLongLines);

        // only print overtime if we don't search for specific items
        // In this case overtime is just confusing
        if (searchString == null || searchString.isEmpty()) {
            printOvertime(printTo, reportStart, reportEnd);
        }
    }

    private void printOvertime(PrintStream printTo, LocalDate reportStart,
                               LocalDate reportEnd) {
        OvertimeReportGenerator overtimeReportGenerator = createOvertimeReportGenerator();
        Map<LocalDate, Duration> overtimeMap = overtimeReportGenerator
                .getOvertime(reportStart, reportEnd);
        Duration overallOvertime = overtimeReportGenerator.getOverallOvertime();

        if (DateTimes.isToday(reportStart)) {
            printTo.println("====== times for today: ======");
            Duration duration = overtimeMap.get(LocalDate.now());
            if (duration != null) {
                String closingTime = DateTimes.prettyPrintTime(LocalDateTime
                        .now().minus(duration));
                printTo.println("closing time: " + closingTime);
                String timeToGo = DateTimes
                        .prettyPrintDuration(duration.negated());
                printTo.println("time to go:   " + timeToGo);
            }

        } else {
            printTo.println("====== overtime from "
                    + DateTimes.prettyPrintDate(reportStart) + " to "
                    + DateTimes.prettyPrintDate(reportEnd) + ": ======");
            Duration overallDuration = Duration.ZERO;
            for (Map.Entry<LocalDate, Duration> e : overtimeMap.entrySet()) {
                overallDuration = overallDuration.plus(e.getValue());

                printTo.println(DateTimes.prettyPrintDate(e.getKey())
                        + " "
                        + DateTimes.prettyPrintDuration(e.getValue())
                        + " overall: "
                        + DateTimes.prettyPrintDuration(overallDuration));
            }
            printTo.print("sum:       ");
            printTo.println(DateTimes.prettyPrintDuration(overallDuration));
        }
        printTo.println("overall overtime: "
                + DateTimes.prettyPrintDuration(overallOvertime));

    }

    /**
     * Prints a nice summed and grouped (by comment) report
     */
    private void printSums(PrintStream printTo, String searchString,
                           LocalDate reportStart, LocalDate reportEnd, boolean truncateLongLines) {
        Criteria criteria = new Criteria();
        if (searchString != null) {
            criteria.withActivityContains(searchString);
        }
        criteria.withStartBetween(Interval.between(reportStart, reportEnd));

        try (Stream<TimeTrackingItem> itemsToConsider = queries.queryItems(criteria)) {
            SummingReportGenerator reporter = new SummingReportGenerator(itemsToConsider);
            Report report = reporter.createReport();

            if (DateTimes.isToday(reportStart)) {
                printTo.println("====== sums of today ======");
                if (report.getStart() != null) {
                    printTo.println("start of day: "
                            + DateTimes.prettyPrintTime(report.getStart()));
                }
                if (report.getEnd() != null) {
                    printTo.println("end of day:   "
                            + DateTimes.prettyPrintTime(report.getEnd()));
                }
            } else {
                printTo.println("====== sums from "
                        + DateTimes.prettyPrintDate(reportStart) + " to "
                        + DateTimes.prettyPrintDate(reportEnd));
            }
            if (!report.getUncoveredDuration().equals(Duration.ZERO)) {
                printTo.println("time not yet tracked: "
                        + DateTimes.prettyPrintDuration(report
                        .getUncoveredDuration()));
            }
            List<ReportingItem> reportingItems = report.getReportingItems();

            Duration worktimeDuration = Duration.ZERO;
            Duration breakTimeDuration = Duration.ZERO;
            for (ReportingItem i : reportingItems) {
                Duration duration = i.getDuration();
                String comment = i.getComment();
                String prefix = " ";
                if (ItemCategory.BREAK.equals(categorizer.getCategory(comment))) {
                    prefix = "*";
                    breakTimeDuration = breakTimeDuration.plus(duration);
                } else {
                    worktimeDuration = worktimeDuration.plus(duration);
                }
                printTruncatedString(
                        prefix + DateTimes.prettyPrintDuration(duration)
                                + "   " + comment, printTo, truncateLongLines);
            }

            printTo.println("====== overall sum: ======");
            printTo.println("work:  "
                    + DateTimes.prettyPrintDuration(worktimeDuration));
            printTo.println("break: "
                    + DateTimes.prettyPrintDuration(breakTimeDuration));
        }
    }

    /**
     * Prints all items nicely formatted
     */
    private void printDetails(PrintStream printTo, String searchString,
                              LocalDate reportStart, LocalDate reportEnd, boolean truncateLongLines) {

        printTo.println("====== recorded items: ======");

        Criteria criteria = new Criteria()
                .withStartBetween(Interval.between(reportStart, reportEnd));
        try (Stream<TimeTrackingItem> itemStream = queries.queryItems(criteria)) {
            itemStream.forEach(item -> {
                LocalDateTime start = item.getStart();
                LocalDateTime end = item.getEnd().orElse(null);
                String comment = item.getActivity();

                StringBuilder builder = new StringBuilder();
                builder.append(DateTimes.prettyPrintTime(start));
                builder.append(" - ");
                if (end == null) {
                    builder.append("now     ");
                } else {
                    builder.append(DateTimes.prettyPrintTime(end));
                }
                builder.append(" ( ");
                builder.append(DateTimes.prettyPrintDuration(Duration.between(
                        start, end == null ? LocalDateTime.now() : end)));
                builder.append(" ) ");
                builder.append(" => ");
                builder.append(comment);
                if (searchString == null
                        || builder.toString().contains(searchString)) {
                    printTruncatedString(builder, printTo, truncateLongLines);
                }
            });
        }
    }

    private OvertimeReportGenerator createOvertimeReportGenerator() {
        return new OvertimeReportGenerator(queries, categorizer,
                workingtimeItemProvider);
    }

    private void printTruncatedString(StringBuilder toPrint,
                                      PrintStream printTo, boolean doTruncate) {
        printTruncatedString(toPrint.toString(), printTo, doTruncate);
    }

    private void printTruncatedString(String toPrint, PrintStream printTo,
                                      boolean doTruncate) {

        int desiredWidth = Math.max(configuration.getCliReportingWidth() - 3,
                10);
        if (doTruncate && desiredWidth < toPrint.length()) {
            String substr = toPrint.substring(0, desiredWidth);
            printTo.println(substr + "...");
        } else {
            printTo.println(toPrint);
        }
    }
}
