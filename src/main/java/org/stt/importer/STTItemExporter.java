package org.stt.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

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
		try (BufferedReader in = new BufferedReader(support.provideReader());
				PrintWriter out = new PrintWriter(stringWriter)) {
			String line;
			boolean newItemHasBeenWritten = false;
			while ((line = in.readLine()) != null) {
				TimeTrackingItem readItem = converter
						.lineToTimeTrackingItem(line);
				boolean readItemStartedBeforeNewItem = readItem.getStart()
						.isBefore(item.getStart());
				boolean readItemEndsWithinNewItemsInterval = readItem.getEnd()
						.isPresent()
						&& (!item.getEnd().isPresent() || isWithin(readItem
								.getEnd().get(), item.getStart(), item.getEnd()
								.get()));
				boolean readItemEndsAfterNewItemsEnd = item.getEnd()
						.isPresent()
						&& (!readItem.getEnd().isPresent() || readItem.getEnd()
								.get().isAfter(item.getEnd().get()));

				if (readItemStartedBeforeNewItem) {
					if (!readItem.getEnd().isPresent()
							|| readItemEndsWithinNewItemsInterval) {
						TimeTrackingItem readItemWithEnd = readItem
								.withEnd(item.getStart());
						out.println(converter
								.timeTrackingItemToLine(readItemWithEnd));
					} else {
						out.println(line);
					}
				} else if (!newItemHasBeenWritten) {
					out.println(converter.timeTrackingItemToLine(item));
					newItemHasBeenWritten = true;
				}
				if (readItemEndsAfterNewItemsEnd) {
					TimeTrackingItem readItemWithNewStart = readItem
							.withStart(item.getEnd().get());
					out.println(converter
							.timeTrackingItemToLine(readItemWithNewStart));
				}
			}
			if (!newItemHasBeenWritten)
				out.println(converter.timeTrackingItemToLine(item));
		}
		rewriteFileWith(stringWriter.toString());
	}

	private boolean isWithin(DateTime dateTime, DateTime start, DateTime end) {
		Interval interval = new Interval(start, end);
		return interval.contains(dateTime);
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

}
