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

	public Main(ItemWriter writeTo, ItemReader readFrom, ItemSearcher searchIn) {
		this.writeTo = checkNotNull(writeTo);
		this.readFrom = checkNotNull(readFrom);
		this.searchIn = checkNotNull(searchIn);
	}

	void on(String[] args) {
		StringBuilder comment = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			comment.append(args[i]);
		}

		ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
				writeTo, searchIn);
		tiw.executeCommand(comment.toString());
	}

	private void report(String[] args) {
		Optional<TimeTrackingItem> item;
		while ((item = readFrom.read()).isPresent()) {
			System.out.println(item);
		}
	}

	private void fin() throws IOException {

		Optional<TimeTrackingItem> item = null;
		Optional<TimeTrackingItem> newitem;
		// get the last item
		while ((newitem = readFrom.read()).isPresent()) {
			item = newitem;
		}

		TimeTrackingItem last = item.orNull();
		if (last != null) {
			TimeTrackingItem newLast = new TimeTrackingItem(last.getComment()
					.orNull(), last.getStart(), DateTime.now());

			writeTo.delete(last);
			writeTo.write(newLast);
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

		ItemReaderProvider itemReaderProvider = new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				try {
					return createNewReader();
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		};
		

		DefaultItemExporter exporter = new DefaultItemExporter(new StreamResourceProvider() {
			
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
		});
		DefaultItemImporter importer = createNewReader();
		DefaultItemSearcher searcher = new DefaultItemSearcher(itemReaderProvider);

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

	private static DefaultItemImporter createNewReader()
			throws FileNotFoundException {
		return new DefaultItemImporter(new FileReader(timeFile));
	}
}
