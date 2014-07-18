package org.stt.cli;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.Configuration;
import org.stt.DateTimeHelper;
import org.stt.filter.StartDateReaderFilter;
import org.stt.filter.SubstringReaderFilter;
import org.stt.g4.EnglishCommandsLexer;
import org.stt.g4.EnglishCommandsParser;
import org.stt.g4.EnglishCommandsParser.ReportStartContext;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.ItemCategorizer;
import org.stt.reporting.OvertimeReportGenerator;
import org.stt.reporting.SummingReportGenerator;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.reporting.WorkingtimeItemProvider;
import org.stt.reporting.WorktimeCategorizer;
import org.stt.stt.importer.CachingItemReader;

import com.google.common.base.Optional;

/**
 * Prints a nicely formatted report of {@link TimeTrackingItem}s
 */
public class ReportPrinter {

	private final ItemReaderProvider readFrom;
	private final Configuration configuration;
	private final WorkingtimeItemProvider workingtimeItemProvider;

	public ReportPrinter(ItemReaderProvider readFrom,
			Configuration configuration,
			WorkingtimeItemProvider workingtimeItemProvider) {
		this.readFrom = readFrom;
		this.configuration = configuration;
		this.workingtimeItemProvider = workingtimeItemProvider;
	}

	public void report(Collection<String> args, PrintStream printTo) {
		String searchString = null;
		DateTime reportStart = DateTime.now().withTimeAtStartOfDay();
		DateTime reportEnd = DateTime.now().plusDays(1).withTimeAtStartOfDay()
				.minus(1);
		boolean truncateLongLines = true;

		if (args.size() > 0) {
			// there is a parameter! Let's parse it ;-)

			truncateLongLines = !args.remove("long");

			// first collapse all following strings
			String argsString = StringHelper.join(args);

			EnglishCommandsLexer lexer = new EnglishCommandsLexer(
					new ANTLRInputStream(argsString));
			EnglishCommandsParser parser = new EnglishCommandsParser(
					new CommonTokenStream(lexer));

			ReportStartContext startContext = parser.reportStart();
			if (startContext.from_date != null) {
				reportStart = startContext.from_date;
				reportEnd = startContext.to_date.plusDays(1)
						.withTimeAtStartOfDay().minus(1);
			} else {
				searchString = argsString;
			}
		}

		String output = "output " + (truncateLongLines ? "" : "not ")
				+ "truncated lines for ";
		if (DateTimeHelper.isToday(reportStart)) {
			output += "today ";

		} else {
			output += DateTimeHelper.prettyPrintDate(reportStart) + " to "
					+ DateTimeHelper.prettyPrintDate(reportEnd);
		}
		printTo.println(output);

		printDetails(printTo, searchString, reportStart, reportEnd,
				truncateLongLines);

		printSums(printTo, searchString, reportStart, reportEnd,
				truncateLongLines);

		printOvertime(printTo, reportStart, reportEnd);
	}

	private void printOvertime(PrintStream printTo, DateTime reportStart,
			DateTime reportEnd) {
		OvertimeReportGenerator overtimeReportGenerator = createOvertimeReportGenerator();
		Map<DateTime, Duration> overtimeMap = overtimeReportGenerator
				.getOvertime(reportStart, reportEnd);
		Duration overallOvertime = overtimeReportGenerator.getOverallOvertime();

		if (DateTimeHelper.isToday(reportStart)) {
			printTo.println("====== times for today: ======");
			Duration duration = overtimeMap.get(DateTime.now()
					.withTimeAtStartOfDay());
			if (duration != null) {
				String closingTime = DateTimeHelper.prettyPrintTime(DateTime
						.now().minus(duration));
				printTo.println("closing time: " + closingTime);
				String timeToGo = DateTimeHelper
						.prettyPrintDuration(new Duration(duration.getMillis()
								* -1));
				printTo.println("time to go:   " + timeToGo);
			}

		} else {
			printTo.println("====== overtime from "
					+ DateTimeHelper.prettyPrintDate(reportStart) + " to "
					+ DateTimeHelper.prettyPrintDate(reportEnd) + ": ======");
			Duration overallDuration = new Duration(0);
			for (Map.Entry<DateTime, Duration> e : overtimeMap.entrySet()) {
				overallDuration = overallDuration.plus(e.getValue());

				printTo.println(DateTimeHelper.prettyPrintDate(e.getKey())
						+ " "
						+ DateTimeHelper.prettyPrintDuration(e.getValue())
						+ " overall: "
						+ DateTimeHelper.prettyPrintDuration(overallDuration));
			}
			printTo.print("sum:       ");
			printTo.println(DateTimeHelper.prettyPrintDuration(overallDuration));
		}
		printTo.println("overall overtime: "
				+ DateTimeHelper.prettyPrintDuration(overallOvertime));

	}

	/**
	 * Prints a nice summed and grouped (by comment) report
	 */
	private void printSums(PrintStream printTo, String searchString,
			DateTime reportStart, DateTime reportEnd, boolean truncateLongLines) {
		ItemReader reportReader = readFrom.provideReader();

		SubstringReaderFilter substFilter = new SubstringReaderFilter(
				reportReader, searchString);

		StartDateReaderFilter dateFilter = createStartDateFilterForDays(
				substFilter, reportStart, reportEnd);

		SummingReportGenerator reporter = new SummingReportGenerator(dateFilter);
		Report report = reporter.createReport();

		if (DateTimeHelper.isToday(reportStart)) {
			printTo.println("====== sums of today ======");
			if (report.getStart() != null) {
				printTo.println("start of day: "
						+ DateTimeHelper.prettyPrintTime(report.getStart()));
			}
			if (report.getEnd() != null) {
				printTo.println("end of day:   "
						+ DateTimeHelper.prettyPrintTime(report.getEnd()));
			}
		} else {
			printTo.println("====== sums from "
					+ DateTimeHelper.prettyPrintDate(reportStart) + " to "
					+ DateTimeHelper.prettyPrintDate(reportEnd));
		}
		if (!report.getUncoveredDuration().equals(Duration.ZERO)) {
			printTo.println("time not yet tracked: "
					+ DateTimeHelper.prettyPrintDuration(report
							.getUncoveredDuration()));
		}
		List<ReportingItem> reportingItems = report.getReportingItems();

		Duration overallDuration = new Duration(0);
		for (ReportingItem i : reportingItems) {
			Duration duration = i.getDuration();
			overallDuration = overallDuration.plus(duration);
			String comment = i.getComment();
			printTruncatedString(DateTimeHelper.prettyPrintDuration(duration)
					+ "   " + comment, printTo, truncateLongLines);
		}

		printTo.println("====== overall sum: ======\n"
				+ DateTimeHelper.prettyPrintDuration(overallDuration));

		IOUtils.closeQuietly(reportReader);
	}

	/**
	 * Prints all items nicely formatted
	 */
	private void printDetails(PrintStream printTo, String searchString,
			DateTime reportStart, DateTime reportEnd, boolean truncateLongLines) {

		printTo.println("====== recorded items: ======");

		ItemReader detailsReader = readFrom.provideReader();
		ItemReader filteredReader = createStartDateFilterForDays(detailsReader,
				reportStart, reportEnd);
		Optional<TimeTrackingItem> optionalItem;
		while ((optionalItem = filteredReader.read()).isPresent()) {
			TimeTrackingItem item = optionalItem.get();
			DateTime start = item.getStart();
			DateTime end = item.getEnd().orNull();
			String comment = item.getComment().orNull();

			StringBuilder builder = new StringBuilder();
			builder.append(DateTimeHelper.prettyPrintTime(start));
			builder.append(" - ");
			if (end == null) {
				builder.append("now     ");
			} else {
				builder.append(DateTimeHelper.prettyPrintTime(end));
			}
			builder.append(" ( ");
			builder.append(DateTimeHelper.prettyPrintDuration(new Duration(
					start, (end == null ? DateTime.now() : end))));
			builder.append(" ) ");
			builder.append(" => ");
			builder.append(comment);
			if (searchString == null
					|| builder.toString().contains(searchString)) {
				printTruncatedString(builder, printTo, truncateLongLines);
			}
		}
		IOUtils.closeQuietly(filteredReader);
	}

	private OvertimeReportGenerator createOvertimeReportGenerator() {
		ItemCategorizer categorizer = new WorktimeCategorizer(configuration);
		CachingItemReader cacher = new CachingItemReader(
				readFrom.provideReader());
		return new OvertimeReportGenerator(cacher, categorizer,
				workingtimeItemProvider);
	}

	/**
	 * Creates a filter which only accepts items with start date of
	 * "today minus the given days" to "today"
	 */
	private StartDateReaderFilter createStartDateFilterForDays(
			ItemReader readerToFilter, DateTime start, DateTime end) {
		StartDateReaderFilter dateFilter = new StartDateReaderFilter(
				readerToFilter, start, end);
		return dateFilter;
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
