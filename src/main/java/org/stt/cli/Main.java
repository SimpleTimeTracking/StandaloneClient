package org.stt.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.Configuration;
import org.stt.ToItemWriterCommandHandler;
import org.stt.filter.StartDateReaderFilter;
import org.stt.filter.SubstringReaderFilter;
import org.stt.importer.AggregatingImporter;
import org.stt.importer.DefaultItemExporter;
import org.stt.importer.DefaultItemImporter;
import org.stt.importer.StreamResourceProvider;
import org.stt.importer.ti.TiImporter;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemSearcher;
import org.stt.persistence.ItemWriter;
import org.stt.reporting.ReportGenerator;
import org.stt.reporting.SummingReportGenerator;
import org.stt.searching.DefaultItemSearcher;

import com.google.common.base.Optional;

/**
 * The starting point for the CLI
 */
public class Main {

	private static Logger LOG = Logger.getLogger(Main.class.getName());

	private final Configuration configuration;

	private final File timeFile;
	private final File tiFile;
	private final File currentTiFile;

	private ItemWriter writeTo;
	private ItemReader readFrom;
	private ItemSearcher searchIn;

	private final DateTimeFormatter hmsDateFormat = DateTimeFormat
			.forPattern("HH:mm:ss");
	private final DateTimeFormatter mdhmsDateFormat = DateTimeFormat
			.forPattern("MM-dd HH:mm:ss");

	private final PeriodFormatter hmsPeriodFormatter = new PeriodFormatterBuilder()
			.printZeroAlways().minimumPrintedDigits(2).appendHours()
			.appendSuffix("h").appendSeparator(":").appendMinutes()
			.appendSuffix("m").appendSeparator(":").appendSeconds()
			.appendSuffix("s").toFormatter();

	public Main(Configuration configuration) {
		this.configuration = checkNotNull(configuration);

		timeFile = configuration.getSttFile();
		tiFile = configuration.getTiFile();
		currentTiFile = configuration.getTiCurrentFile();
	}

	private void on(String[] args) throws IOException {
		String comment = join(1, args);

		ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
				writeTo, searchIn);
		Optional<TimeTrackingItem> createdItem = tiw.executeCommand(comment);

		// FIXME: sysout("stopped working on $old_item")
		System.out.println("start working on "
				+ createdItem.get().getComment().orNull());
		tiw.close();
	}

	/**
	 * @param args
	 * @param comment
	 */
	private String join(int start, String[] args) {

		StringBuilder builder = new StringBuilder();
		for (int i = start; i < args.length; i++) {
			builder.append(args[i]);
			if (i < args.length - 1) {
				builder.append(' ');
			}
		}
		return builder.toString();
	}

	private void report(String[] args) throws IOException {
		String searchString = null;
		int days = 0;
		if (args.length > 1) {
			// there is a parameter! Let's parse it ;-)

			// first collapse all following strings
			String argsString = join(1, args);

			Pattern daysPattern = Pattern.compile("(\\d+) days");
			Matcher daysMatcher = daysPattern.matcher(argsString);

			if (daysMatcher.find()) {
				days = Integer.parseInt(daysMatcher.group(1));
			} else {
				searchString = argsString;
			}
		}

		// FIXME: implement
		// - "11 days": all items from 11 days ago, grouped by comment and
		// summed by durations
		// - "yesterday": all items of yesterday, grouped by comment and summed
		// by durations
		// - "week": all items of this week, grouped by comment and summed by
		// durations
		// - "last week": all items of last week, grouped by comment and summed
		// by durations
		// - "year": all items of this year, grouped by comment and summed by
		// durations

		ItemReader filteredReader = new StartDateReaderFilter(readFrom,
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
				printTruncatedString(builder);
			}
		}
		filteredReader.close();

		// create a new reader and output the summed up report
		if (days > 0) {
			System.out.println("====== sums of the last " + days
					+ " days ======");
		} else {
			System.out.println("====== sums of today ======");
		}
		ItemReader reportReader = createNewReader();
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
					+ "   " + comment);
		}

		System.out.println("====== overall sum: ======\n"
				+ hmsPeriodFormatter.print(overallDuration.toPeriod()));

		reportReader.close();
	}

	/**
	 * output all items where the comment contains (ignoring case) the given
	 * args.
	 * 
	 * Only unique comments are printed.
	 * 
	 * The ordering of the output is from newest to oldest
	 */
	private void search(String[] args) throws IOException {

		SortedSet<TimeTrackingItem> sortedItems = new TreeSet<TimeTrackingItem>(
				new Comparator<TimeTrackingItem>() {

					@Override
					public int compare(TimeTrackingItem o1, TimeTrackingItem o2) {
						return o2.getStart().compareTo(o1.getStart());
					}
				});

		ItemReader reader = new SubstringReaderFilter(readFrom, join(1, args));
		sortedItems.addAll(IOUtil.readAll(reader));

		Set<String> sortedUniqueComments = new HashSet<>(sortedItems.size());

		for (TimeTrackingItem i : sortedItems) {
			String comment = i.getComment().orNull();
			if (comment != null && !sortedUniqueComments.contains(comment)) {
				sortedUniqueComments.add(comment);
				System.out.println(comment);
			}
		}
	}

	private void fin() throws IOException {
		try (ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
				writeTo, searchIn)) {
			Optional<TimeTrackingItem> updatedItem = tiw
					.executeCommand(ToItemWriterCommandHandler.COMMAND_FIN);
			if (updatedItem.isPresent()) {
				System.out.println("stopped working on "
						+ updatedItem.get().toString());
			}
		}
	}

	private void printTruncatedString(StringBuilder toPrint) {
		printTruncatedString(toPrint.toString());
	}

	private void printTruncatedString(String toPrint) {
		int desiredWidth = configuration.getCliReportingWidth() - 3;
		if (desiredWidth < toPrint.length()) {
			String substr = toPrint.substring(0, desiredWidth);
			System.out.println(substr + "...");
		} else {
			System.out.println(toPrint);
		}
	}

	/*
	 * 
	 * CLI use (example from ti usage):
	 * 
	 * ti on long text containing comment //starts a new entry and inserts
	 * comment
	 * 
	 * ti on other comment //sets end time to the previous item and starts the
	 * new one
	 * 
	 * ti fin // sets end time of previous item
	 */
	public static void main(String[] args) throws IOException {
		// apply the desired encoding for all System.out calls
		// this is necessary if one wants to output non ASCII
		// characters on a Windows console
		Configuration configuration = new Configuration();
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out),
				true, configuration.getSystemOutEncoding()));

		Main m = new Main(configuration);

		m.executeCommand(args);
	}

	private void executeCommand(String... args) {
		if (args.length == 0) {
			usage();
			return;
		}

		try (DefaultItemExporter exporter = new DefaultItemExporter(
				createPersistenceStreamSupport());
				ItemReader importer = createNewReader()) {
			DefaultItemSearcher searcher = createNewSearcher();

			writeTo = exporter;
			readFrom = importer;
			searchIn = searcher;

			String mainOperator = args[0];
			if (mainOperator.startsWith("o")) {
				// on
				on(args);
			} else if (mainOperator.startsWith("r")) {
				// report
				report(args);
			} else if (mainOperator.startsWith("f")) {
				// fin
				fin();
			} else if (mainOperator.startsWith("s")) {
				// search
				search(args);
			} else {
				usage();
			}
		} catch (IOException e) {
			LOG.throwing(Main.class.getName(), "executeCommand", e);
		}
	}

	/**
	 * Prints usage information to stdout
	 */
	private static void usage() {
		String usage = "Usage:\n"
				+ "on comment\tto start working on something\n"
				+ "report [X days] [searchstring]\tto display a report\n"
				+ "fin\t\tto stop working\n"
				+ "search [searchstring]\tto get a list of all comments of items matching the given search string";

		System.out.println(usage);
	}

	private DefaultItemSearcher createNewSearcher() {
		return new DefaultItemSearcher(new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				try {
					return createNewReader();
				} catch (IOException e) {
					LOG.log(Level.SEVERE, "Error creating reader", e);
					throw new RuntimeException(e);
				}
			}
		});
	}

	/**
	 * For each existing file create a reader and return an AggregatingImporter
	 * of all readers where the corresponding file exists
	 */
	private ItemReader createNewReader() throws IOException {
		List<ItemReader> availableReaders = new LinkedList<>();

		if (timeFile.canRead()) {
			DefaultItemImporter timeImporter = new DefaultItemImporter(
					new InputStreamReader(new FileInputStream(timeFile),
							"UTF-8"));
			availableReaders.add(timeImporter);
		}
		if (tiFile.canRead()) {
			TiImporter tiImporter = new TiImporter(new InputStreamReader(
					new FileInputStream(tiFile), "UTF-8"));
			availableReaders.add(tiImporter);
		}
		if (currentTiFile.canRead()) {
			TiImporter currentTiImporter = new TiImporter(
					new InputStreamReader(new FileInputStream(currentTiFile),
							"UTF-8"));
			availableReaders.add(currentTiImporter);
		}
		AggregatingImporter importer = new AggregatingImporter(
				availableReaders.toArray(new ItemReader[availableReaders.size()]));

		return importer;
	}

	private StreamResourceProvider createPersistenceStreamSupport()
			throws IOException {
		StreamResourceProvider srp = new StreamResourceProvider() {
			private OutputStreamWriter outputStreamWriter;
			private InputStreamReader inReader;
			private OutputStreamWriter appendingOutWriter;

			@Override
			public Writer provideTruncatingWriter() throws IOException {
				outputStreamWriter = new OutputStreamWriter(
						new FileOutputStream(timeFile, false), "UTF-8");
				return outputStreamWriter;
			}

			@Override
			public Reader provideReader() throws IOException {
				inReader = new InputStreamReader(new FileInputStream(timeFile),
						"UTF-8");
				return inReader;
			}

			@Override
			public Writer provideAppendingWriter() throws IOException {
				appendingOutWriter = new OutputStreamWriter(
						new FileOutputStream(timeFile, true), "UTF-8");
				return appendingOutWriter;
			}

			@Override
			public void close() {
				IOUtils.closeQuietly(outputStreamWriter);
				IOUtils.closeQuietly(inReader);
				IOUtils.closeQuietly(appendingOutWriter);
			}
		};
		return srp;
	}
}
