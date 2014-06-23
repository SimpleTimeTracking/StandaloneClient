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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.stt.Configuration;
import org.stt.ToItemWriterCommandHandler;
import org.stt.filter.SubstringReaderFilter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemWriter;
import org.stt.searching.DefaultItemSearcher;
import org.stt.searching.ItemSearcher;
import org.stt.stt.importer.AggregatingImporter;
import org.stt.stt.importer.STTItemExporter;
import org.stt.stt.importer.STTItemImporter;
import org.stt.stt.importer.StreamResourceProvider;
import org.stt.ti.importer.TiImporter;

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

	public Main(Configuration configuration) {
		this.configuration = checkNotNull(configuration);

		timeFile = configuration.getSttFile();
		tiFile = configuration.getTiFile();
		currentTiFile = configuration.getTiCurrentFile();
	}

	private void on(Collection<String> args, PrintStream printTo)
			throws IOException {
		String comment = StringHelper.join(args);

		Optional<TimeTrackingItem> currentItem = searchIn
				.getCurrentTimeTrackingitem();

		ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
				writeTo, searchIn);
		Optional<TimeTrackingItem> createdItem = tiw.executeCommand(comment);

		if (currentItem.isPresent()) {
			printTo.println("stopped working on " + currentItem);
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

		SortedSet<TimeTrackingItem> sortedItems = new TreeSet<TimeTrackingItem>(
				new Comparator<TimeTrackingItem>() {

					@Override
					public int compare(TimeTrackingItem o1, TimeTrackingItem o2) {
						return o2.getStart().compareTo(o1.getStart());
					}
				});

		ItemReader reader = new SubstringReaderFilter(readFrom,
				StringHelper.join(args));
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

	private void fin(PrintStream printTo) throws IOException {
		try (ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
				writeTo, searchIn)) {
			Optional<TimeTrackingItem> updatedItem = tiw
					.executeCommand(ToItemWriterCommandHandler.COMMAND_FIN);
			if (updatedItem.isPresent()) {
				printTo.println("stopped working on "
						+ updatedItem.get().toString());
			}
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
		List<String> argsList = new ArrayList<>(Arrays.asList(args));
		m.executeCommand(argsList, System.out);
	}

	void executeCommand(List<String> args, PrintStream printTo) {
		if (args.size() == 0) {
			usage(printTo);
			return;
		}

		try (STTItemExporter exporter = new STTItemExporter(
				createPersistenceStreamSupport());
				ItemReader importer = createNewReader()) {
			DefaultItemSearcher searcher = createNewSearcher();

			writeTo = exporter;
			readFrom = importer;
			searchIn = searcher;

			String mainOperator = args.remove(0);
			if (mainOperator.startsWith("o")) {
				// on
				on(args, printTo);
			} else if (mainOperator.startsWith("r")) {
				// report
				createNewReportPrinter().report(args, printTo);
			} else if (mainOperator.startsWith("f")) {
				// fin
				fin(printTo);
			} else if (mainOperator.startsWith("s")) {
				// search
				search(args, printTo);
			} else {
				usage(printTo);
			}
		} catch (IOException e) {
			LOG.throwing(Main.class.getName(), "executeCommand", e);
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

	private DefaultItemSearcher createNewSearcher() {
		return new DefaultItemSearcher(new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				try {
					return createNewReader();
				} catch (IOException e) {
					LOG.throwing(Main.class.getName(), "createNewSearcher", e);
					throw new RuntimeException(e);
				}
			}
		});
	}

	private ReportPrinter createNewReportPrinter() {
		ItemReaderProvider provider = new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				try {
					return createNewReader();
				} catch (IOException e) {
					LOG.throwing(Main.class.getName(),
							"createNewReportPrinter", e);
					throw new RuntimeException(e);
				}
			}
		};
		return new ReportPrinter(provider, searchIn, configuration);
	}

	/**
	 * For each existing file create a reader and return an AggregatingImporter
	 * of all readers where the corresponding file exists
	 */
	private ItemReader createNewReader() throws IOException {
		List<ItemReader> availableReaders = new LinkedList<>();

		if (timeFile.canRead()) {
			STTItemImporter timeImporter = new STTItemImporter(
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
