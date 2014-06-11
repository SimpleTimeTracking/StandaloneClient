package org.stt.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.ToItemWriterCommandHandler;
import org.stt.importer.DefaultItemExporter;
import org.stt.importer.DefaultItemImporter;
import org.stt.importer.StreamResourceProvider;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemSearcher;
import org.stt.persistence.ItemWriter;
import org.stt.searching.DefaultItemSearcher;

import com.google.common.base.Optional;

/**
 * The starting point for the CLI
 */
public class Main {

	private static final File timeFile = new File(
			System.getProperty("user.home"), ".stt");

	private final ItemWriter writeTo;
	private final ItemReader readFrom;
	private final ItemSearcher searchIn;

	private final DateTimeFormatter hmsDateFormat = DateTimeFormat
			.forPattern("HH:mm:ss");

	public Main(ItemWriter writeTo, ItemReader readFrom, ItemSearcher searchIn) {
		this.writeTo = checkNotNull(writeTo);
		this.readFrom = checkNotNull(readFrom);
		this.searchIn = checkNotNull(searchIn);
	}

	void on(String[] args) throws IOException {
		StringBuilder comment = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			comment.append(args[i]);
			if (i < args.length - 1) {
				comment.append(' ');
			}
		}

		ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
				writeTo, searchIn);
		tiw.executeCommand(comment.toString());
		tiw.close();
	}

	private void report(String[] args) {
		String searchString = null;
		if (args.length > 1) {
			// there is a parameter! Let's parse it ;-)
			searchString = args[1];
		}
		Optional<TimeTrackingItem> optionalItem;
		while ((optionalItem = readFrom.read()).isPresent()) {
			TimeTrackingItem item = optionalItem.get();
			DateTime start = item.getStart();
			DateTime end = item.getEnd().orNull();
			String comment = item.getComment().orNull();

			StringBuilder builder = new StringBuilder();
			builder.append(hmsDateFormat.print(start));
			builder.append(" - ");
			if (end == null) {
				builder.append("        ");
			} else {
				builder.append(hmsDateFormat.print(end));
			}
			builder.append(" => ");
			builder.append(comment);

			if (searchString == null
					|| builder.toString().contains(searchString)) {
				System.out.println(builder.toString());
			}
		}
	}

	private void fin() throws IOException {

		TimeTrackingItem current = searchIn.getCurrentTimeTrackingitem()
				.orNull();
		if (current != null) {
			writeTo.replace(current, current.withEnd(DateTime.now()));
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
		if (args.length == 0) {
			String help = "Usage:\non $comment\tto start working on something\n"
					+ "report\t\tto display a report\n"
					+ "fin\t\tto stop working";

			System.out.println(help);
			System.exit(2);
		}

		DefaultItemExporter exporter = new DefaultItemExporter(
				createPersistenceStreamSupport());
		DefaultItemImporter importer = createNewReader();
		DefaultItemSearcher searcher = createNewSearcher();

		Main m = new Main(exporter, importer, searcher);

		String mainOperator = args[0];
		if (mainOperator.startsWith("o")) {
			// on
			m.on(args);
		} else if (mainOperator.startsWith("r")) {
			// report
			m.report(args);
		} else if (mainOperator.startsWith("f")) {
			// fin
			m.fin();
		}

		exporter.close();
		importer.close();
	}

	private static DefaultItemSearcher createNewSearcher() {
		return new DefaultItemSearcher(new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				try {
					return createNewReader();
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	private static DefaultItemImporter createNewReader()
			throws FileNotFoundException {
		return new DefaultItemImporter(new FileReader(timeFile));
	}

	private static StreamResourceProvider createPersistenceStreamSupport()
			throws IOException {
		StreamResourceProvider srp = new StreamResourceProvider() {

			@Override
			public Writer provideTruncatingWriter() throws IOException {
				return new FileWriter(timeFile, false);
			}

			@Override
			public Reader provideReader() throws FileNotFoundException {
				return new FileReader(timeFile);
			}

			@Override
			public Writer provideAppendingWriter() throws IOException {
				return new FileWriter(timeFile, true);
			}

			@Override
			public void close() {

			}
		};
		return srp;
	}
}
