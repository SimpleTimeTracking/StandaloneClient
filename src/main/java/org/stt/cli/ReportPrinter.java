package org.stt.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.Configuration;
import org.stt.filter.StartDateReaderFilter;
import org.stt.filter.SubstringReaderFilter;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.ReportGenerator;
import org.stt.reporting.SummingReportGenerator;

import com.google.common.base.Optional;

/**
 * Prints a nicely formatted report of {@link TimeTrackingItem}s
 */
public class ReportPrinter {

	private final DateTimeFormatter hmsDateFormat = DateTimeFormat
			.forPattern("HH:mm:ss");
	private final DateTimeFormatter mdhmsDateFormat = DateTimeFormat
			.forPattern("MM-dd HH:mm:ss");

	private final PeriodFormatter hmsPeriodFormatter = new PeriodFormatterBuilder()
			.printZeroAlways().minimumPrintedDigits(2).appendHours()
			.appendSuffix("h").appendSeparator(":").appendMinutes()
			.appendSuffix("m").appendSeparator(":").appendSeconds()
			.appendSuffix("s").toFormatter();

	private ItemReaderProvider readFrom;
	private Configuration configuration;

	public ReportPrinter(ItemReaderProvider readFrom,
			Configuration configuration) {
		this.readFrom = readFrom;
		this.configuration = configuration;
	}

	public void report(Collection<String> args, PrintStream printTo)
			throws IOException {
		String searchString = null;
		int days = 0;
		boolean truncateLongLines = true;

		if (args.size() > 0) {
			// there is a parameter! Let's parse it ;-)

			// first collapse all following strings
			String argsString = StringHelper.join(args);

			if (argsString.endsWith("long")) {
				argsString = argsString.replaceAll("long$", "");
				truncateLongLines = false;
			}

			Pattern daysPattern = Pattern.compile("(\\d+) days");
			Matcher daysMatcher = daysPattern.matcher(argsString);

			if (daysMatcher.find()) {
				days = Integer.parseInt(daysMatcher.group(1));
			} else {
				searchString = argsString;
			}
		}

		// TODO: implement
		// - "yesterday": all items of yesterday, grouped by comment and summed
		// by durations
		// - "week": all items of this week, grouped by comment and summed by
		// durations
		// - "last week": all items of last week, grouped by comment and summed
		// by durations
		// - "year": all items of this year, grouped by comment and summed by
		// durations
		printDetails(printTo, searchString, days, truncateLongLines);

		// create a new reader and output the summed up report
		printSums(printTo, searchString, days, truncateLongLines);
	}

	/**
	 * Prints a nice summed and grouped (by comment) report
	 */
	private void printSums(PrintStream printTo, String searchString, int days,
			boolean truncateLongLines) throws IOException {
		if (days > 0) {
			printTo.println("====== sums of the last " + days + " days ======");
		} else {
			printTo.println("====== sums of today ======");
		}
		ItemReader reportReader = readFrom.provideReader();
		ReportGenerator reporter = new SummingReportGenerator(
				new StartDateReaderFilter(new SubstringReaderFilter(
						reportReader, searchString), DateTime.now()
						.withTimeAtStartOfDay().minusDays(days).toDateTime(),
						DateTime.now().withTimeAtStartOfDay().plusDays(1)
								.toDateTime()));
		List<ReportingItem> report = reporter.report();

		Duration overallDuration = new Duration(0);
		for (ReportingItem i : report) {
			Duration duration = i.getDuration();
			overallDuration = overallDuration.plus(duration);
			String comment = i.getComment();
			printTruncatedString(hmsPeriodFormatter.print(duration.toPeriod())
					+ "   " + comment, printTo, truncateLongLines);
		}

		printTo.println("====== overall sum: ======\n"
				+ hmsPeriodFormatter.print(overallDuration.toPeriod()));

		reportReader.close();
	}

	/**
	 * Prints all items nicely formatted
	 */
	private void printDetails(PrintStream printTo, String searchString,
			int days, boolean truncateLongLines) throws IOException {
		ItemReader detailsReader = readFrom.provideReader();
		ItemReader filteredReader = new StartDateReaderFilter(detailsReader,
				DateTime.now().withTimeAtStartOfDay().minusDays(days)
						.toDateTime(), DateTime.now().withTimeAtStartOfDay()
						.plusDays(1).toDateTime());
		Optional<TimeTrackingItem> optionalItem;
		while ((optionalItem = filteredReader.read()).isPresent()) {
			TimeTrackingItem item = optionalItem.get();
			DateTime start = item.getStart();
			DateTime end = item.getEnd().orNull();
			String comment = item.getComment().orNull();

			StringBuilder builder = new StringBuilder();
			if (start.getYear() == DateTime.now().getYear()
					&& start.getDayOfYear() == DateTime.now().getDayOfYear()) {
				builder.append(hmsDateFormat.print(start));
			} else {
				builder.append(mdhmsDateFormat.print(start));
			}
			builder.append(" - ");
			if (end == null) {
				builder.append("now     ");
			} else {
				builder.append(hmsDateFormat.print(end));
			}
			builder.append(" ( ");
			builder.append(hmsPeriodFormatter.print(new Duration(start,
					(end == null ? DateTime.now() : end)).toPeriod()));
			builder.append(" ) ");
			builder.append(" => ");
			builder.append(comment);
			if (searchString == null
					|| builder.toString().contains(searchString)) {
				printTruncatedString(builder, printTo, truncateLongLines);
			}
		}
		filteredReader.close();
	}

	private void printTruncatedString(StringBuilder toPrint,
			PrintStream printTo, boolean doTruncate) {
		printTruncatedString(toPrint.toString(), printTo, doTruncate);
	}

	private void printTruncatedString(String toPrint, PrintStream printTo,
			boolean doTruncate) {

		int desiredWidth = configuration.getCliReportingWidth() - 3;
		if (doTruncate && desiredWidth < toPrint.length()) {
			String substr = toPrint.substring(0, desiredWidth);
			printTo.println(substr + "...");
		} else {
			printTo.println(toPrint);
		}
	}
}
