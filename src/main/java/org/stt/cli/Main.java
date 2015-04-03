package org.stt.cli;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.inject.Provider;
import org.stt.Configuration;
import org.stt.ToItemWriterCommandHandler;
import org.stt.analysis.WorktimeCategorizer;
import org.stt.filter.SubstringReaderFilter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.*;
import org.stt.persistence.stt.STTItemPersister;
import org.stt.persistence.stt.STTItemReader;
import org.stt.reporting.WorkingtimeItemProvider;
import org.stt.query.DefaultTimeTrackingItemQueries;
import org.stt.query.TimeTrackingItemQueries;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The starting point for the CLI
 */
public class Main {

	private static Logger LOG = Logger.getLogger(Main.class.getName());

	private final Configuration configuration;

	private final File timeFile;

	private ItemPersister itemPersister;
	private TimeTrackingItemQueries timeTrackingItemQueries;

	public Main(Configuration configuration) {
		this.configuration = checkNotNull(configuration);

		timeFile = configuration.getSttFile();
	}

	private void on(Collection<String> args, PrintStream printTo)
			throws IOException {
		String comment = Joiner.on(" ").join(args);

		Optional<TimeTrackingItem> currentItem = timeTrackingItemQueries
				.getCurrentTimeTrackingitem();

		ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
				itemPersister, timeTrackingItemQueries);
		Optional<TimeTrackingItem> createdItem = tiw.executeCommand(comment);

		if (currentItem.isPresent()) {
			prettyPrintTimeTrackingItem(printTo, currentItem);
		}
		printTo.println("start working on "
				+ createdItem.get().getComment().orNull());
		tiw.close();
	}

	/**
	 * output all items where the comment contains (ignoring case) the given
	 * args.
	 * 
	 * Only unique comments are printed.
	 * 
	 * The ordering of the output is from newest to oldest.
	 * 
	 * Useful for completion.
	 */
	private void search(Collection<String> args, PrintStream printTo)
			throws IOException {

		SortedSet<TimeTrackingItem> sortedItems = new TreeSet<>(
				new Comparator<TimeTrackingItem>() {

					@Override
					public int compare(TimeTrackingItem o1, TimeTrackingItem o2) {
						return o2.getStart().compareTo(o1.getStart());
					}
				});

		ItemReader readFrom = createNewReaderProvider(timeFile).provideReader();

		ItemReader reader = new SubstringReaderFilter(readFrom, Joiner.on(" ")
				.join(args));
		sortedItems.addAll(IOUtil.readAll(reader));

		Set<String> sortedUniqueComments = new HashSet<>(sortedItems.size());

		for (TimeTrackingItem i : sortedItems) {
			String comment = i.getComment().orNull();
			if (comment != null && !sortedUniqueComments.contains(comment)) {
				sortedUniqueComments.add(comment);
				printTo.println(comment);
			}
		}
	}

	private void report(List<String> args, PrintStream printTo) {
		File source = timeFile;
		int sourceIndex = args.indexOf("--source");
		if (sourceIndex != -1) {
			args.remove(sourceIndex);
			String sourceParameter = args.get(sourceIndex);
			if (sourceParameter.equals("-")) {
				source = null;
			} else {
				source = new File(sourceParameter);
			}
			args.remove(sourceIndex);
		}

		createNewReportPrinter(source).report(args, printTo);
	}

	private void fin(Collection<String> args, PrintStream printTo)
			throws IOException {
		try (ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
				itemPersister, timeTrackingItemQueries)) {
			Optional<TimeTrackingItem> updatedItem = tiw
					.executeCommand(ToItemWriterCommandHandler.COMMAND_FIN
							+ " " + Joiner.on(" ").join(args));
			if (updatedItem.isPresent()) {
				prettyPrintTimeTrackingItem(printTo, updatedItem);
			}
		}
	}

	/**
	 * @param printTo
	 * @param updatedItem
	 */
	private void prettyPrintTimeTrackingItem(PrintStream printTo,
			Optional<TimeTrackingItem> updatedItem) {
		if (updatedItem.isPresent()) {
			StringBuilder itemString = ItemFormattingHelper
					.prettyPrintItem(updatedItem);
			printTo.println("stopped working on " + itemString.toString());
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

		Main main = new Main(configuration);
		List<String> argsList = new ArrayList<>(Arrays.asList(args));
		main.executeCommand(argsList, System.out);

		// perform backup
		main.createNewBackupCreator(configuration).start();
	}

	void executeCommand(List<String> args, PrintStream printTo) {
		if (args.size() == 0) {
			usage(printTo);
			return;
		}

		try (STTItemPersister itemPersister = new STTItemPersister(
				createReaderProvider(), createWriterProvider())) {

			DefaultTimeTrackingItemQueries searcher = createNewSearcher();

			this.itemPersister = itemPersister;
			timeTrackingItemQueries = searcher;

			String mainOperator = args.remove(0);
			if (mainOperator.startsWith("o")) {
				// on
				on(args, printTo);
			} else if (mainOperator.startsWith("r")) {
				// report
				report(args, printTo);
			} else if (mainOperator.startsWith("f")) {
				// fin
				fin(args, printTo);
			} else if (mainOperator.startsWith("s")) {
				// search
				search(args, printTo);
			} else if (mainOperator.startsWith("c")) {
				// convert
				new FormatConverter(args).convert();
			} else {
				usage(printTo);
			}
		} catch (IOException e) {
			LOG.throwing(Main.class.getName(), "parseCommandString", e);
		}
	}

	/**
	 * Prints usage information to the given Stream
	 */
	private static void usage(PrintStream printTo) {
		String usage = "Usage:\n"
				+ "on comment\tto start working on something\n"
				+ "report [X days] [searchstring]\tto display a report\n"
				+ "fin\t\tto stop working\n"
				+ "search [searchstring]\tto get a list of all comments of items matching the given search string";

		printTo.println(usage);
	}

	private DefaultTimeTrackingItemQueries createNewSearcher() {
		return new DefaultTimeTrackingItemQueries(createNewReaderProvider(timeFile));
	}

	private ReportPrinter createNewReportPrinter(File source) {
		ItemReaderProvider provider = createNewReaderProvider(source);
		return new ReportPrinter(provider, configuration,
				new WorkingtimeItemProvider(configuration),
				new WorktimeCategorizer(configuration));
	}

	/**
	 * creates a new ItemReaderProvider for timeFile
	 */
	private ItemReaderProvider createNewReaderProvider(final File source) {
        ItemReaderProvider provider = new ItemReaderProvider() {
            @Override
            public ItemReader provideReader() {
                try {
                    InputStream inStream;
                    if (source == null) {
                        inStream = System.in;
                    } else {
                        inStream = new FileInputStream(source);
                    }
                    InputStreamReader in = new InputStreamReader(inStream, "UTF-8");
                    return new STTItemReader(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new PreCachingItemReaderProvider(provider);
    }

	private Provider<Reader> createReaderProvider() {
		return new Provider<Reader>() {
			@Override
			public Reader get() {
				try {
					return new InputStreamReader(new FileInputStream(timeFile),
							"UTF-8");
				} catch (UnsupportedEncodingException | FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private Provider<Writer> createWriterProvider() {
		return new Provider<Writer>() {
			@Override
			public Writer get() {
				try {
					return new OutputStreamWriter(
                            new FileOutputStream(timeFile, false), "UTF-8");
				} catch (UnsupportedEncodingException | FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private BackupCreator createNewBackupCreator(Configuration config) {
		return new BackupCreator(config);
	}
}
