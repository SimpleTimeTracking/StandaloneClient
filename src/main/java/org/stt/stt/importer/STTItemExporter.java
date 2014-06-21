package org.stt.stt.importer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Writes {@link TimeTrackingItem}s to a new line. Multiline comments get joined
 * into one line: line endings \r and \n get replaced by the string \r and \n
 * respectively.
 * 
 */
public class STTItemExporter implements ItemWriter {

	private final StreamResourceProvider support;

	private final STTItemConverter converter = new STTItemConverter();

	public STTItemExporter(StreamResourceProvider support) {
		this.support = support;
	}

	@Override
	public void write(TimeTrackingItem item) throws IOException {
		Preconditions.checkNotNull(item);
		StringWriter stringWriter = new StringWriter();
		Reader providedReader;
		try {
			providedReader = support.provideReader();
		} catch (FileNotFoundException e) {
			providedReader = new StringReader("");
		}
		try (BufferedReader in = new BufferedReader(providedReader);
				PrintWriter out = new PrintWriter(stringWriter)) {
			new InsertItemTask(in, out, item).insert();
		}
		rewriteFileWith(stringWriter.toString());
	}

	private boolean isWithin(DateTime dateTime, DateTime start,
			Optional<DateTime> end) {
		if (end.isPresent()) {
			Interval interval = new Interval(start, end.get());
			return interval.contains(dateTime);
		}
		return !dateTime.isBefore(start);
	}

	@Override
	public void replace(TimeTrackingItem item, TimeTrackingItem with)
			throws IOException {
		delete(item);
		write(with);
	}

	@Override
	public void delete(TimeTrackingItem item) throws IOException {

		BufferedReader reader = new BufferedReader(support.provideReader());
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		String currentLine = null;
		while ((currentLine = reader.readLine()) != null) {
			// only write lines which should not be deleted
			if (!currentLine.equals(converter.timeTrackingItemToLine(item))) {
				printWriter.println(currentLine);
			}
		}

		reader.close();
		printWriter.flush();
		rewriteFileWith(stringWriter.toString());
	}

	private void rewriteFileWith(String content) throws IOException {
		Writer truncatingWriter = support.provideTruncatingWriter();
		truncatingWriter.write(content.toString());
		truncatingWriter.close();
	}

	@Override
	public void close() {
		IOUtils.closeQuietly(support);
	}

	public class InsertItemTask {
		private final BufferedReader in;
		private final PrintWriter out;
		private final TimeTrackingItem item;
		private boolean newItemHasBeenWritten = false;

		public InsertItemTask(BufferedReader in, PrintWriter out,
				TimeTrackingItem item) {
			this.in = in;
			this.out = out;
			this.item = item;
		}

		public void insert() throws IOException {
			String line;
			while ((line = in.readLine()) != null) {
				TimeTrackingItem readItem = converter
						.lineToTimeTrackingItem(line);
				insertOrSplitExistingItemAndInsertNewItemIfRequired(line,
						readItem);
			}
			insertItemIfNotYetDone();
		}

		private void insertOrSplitExistingItemAndInsertNewItemIfRequired(
				String line, TimeTrackingItem readItem) throws IOException {
			boolean readItemStartedBeforeNewItem = readItem.getStart()
					.isBefore(item.getStart());
			if (readItemStartedBeforeNewItem) {
				writeExistingItemWithChangedEndIfRequired(line, readItem);
			} else {
				insertItemIfNotYetDone();
				writeExistingItemIfItEndsAfterNewItem(line, readItem);
			}
		}

		private void writeExistingItemIfItEndsAfterNewItem(String line,
				TimeTrackingItem readItem) throws IOException {
			boolean readItemEndsAfterNewItemsEnd = item.getEnd().isPresent()
					&& (!readItem.getEnd().isPresent() || readItem.getEnd()
							.get().isAfter(item.getEnd().get()));
			if (readItemEndsAfterNewItemsEnd) {
				boolean readItemStartsAfterNewItemEnds = item.getEnd()
						.isPresent()
						&& !readItem.getStart().isBefore(item.getEnd().get());
				if (readItemStartsAfterNewItemEnds) {
					out.println(line);
				} else {
					writeExistingItemWithChangedStart(readItem);
				}
			}
		}

		private void writeExistingItemWithChangedStart(TimeTrackingItem readItem)
				throws IOException {
			TimeTrackingItem readItemWithNewStart = readItem.withStart(item
					.getEnd().get());
			out.println(converter.timeTrackingItemToLine(readItemWithNewStart));
		}

		private void writeExistingItemWithChangedEndIfRequired(String line,
				TimeTrackingItem readItem) throws IOException {
			boolean readItemHasEnd = readItem.getEnd().isPresent();
			boolean readItemEndsWithinNewItemsInterval = readItemHasEnd
					&& (isWithin(readItem.getEnd().get(), item.getStart(),
							item.getEnd()));

			if (!readItemHasEnd || readItemEndsWithinNewItemsInterval) {
				TimeTrackingItem readItemWithEnd = readItem.withEnd(item
						.getStart());
				out.println(converter.timeTrackingItemToLine(readItemWithEnd));
			} else {
				out.println(line);
			}
		}

		private void insertItemIfNotYetDone() throws IOException {
			if (!newItemHasBeenWritten) {
				out.println(converter.timeTrackingItemToLine(item));
				newItemHasBeenWritten = true;
			}
		}
	}
}
