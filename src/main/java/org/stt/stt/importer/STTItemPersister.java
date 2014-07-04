package org.stt.stt.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.InsertHelper;
import org.stt.persistence.ItemPersister;

import com.google.common.base.Preconditions;

/**
 * Writes {@link TimeTrackingItem}s to a new line. Multiline comments get joined
 * into one line: line endings \r and \n get replaced by the string \r and \n
 * respectively.
 * 
 */
public class STTItemPersister implements ItemPersister {

	private final StreamResourceProvider support;

	private final STTItemConverter converter = new STTItemConverter();

	public STTItemPersister(StreamResourceProvider support) {
		this.support = support;
	}

	@Override
	public void insert(TimeTrackingItem itemToInsert) throws IOException {
		Preconditions.checkNotNull(itemToInsert);
		StringWriter stringWriter = new StringWriter();
		Reader providedReader;
		providedReader = support.provideReader();
		try (STTItemReader in = new STTItemReader(providedReader);
				STTItemWriter out = new STTItemWriter(stringWriter)) {
			new InsertHelper(in, out, itemToInsert).performInsert();
		}
		rewriteFileWith(stringWriter.toString());
	}

	@Override
	public void replace(TimeTrackingItem item, TimeTrackingItem with)
			throws IOException {
		delete(item);
		insert(with);
	}

	@Override
	public void delete(TimeTrackingItem item) throws IOException {

		BufferedReader reader = new BufferedReader(support.provideReader());
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		String lineOfItemToDelete = converter.timeTrackingItemToLine(item);
		String currentLine = null;
		while ((currentLine = reader.readLine()) != null) {
			// only write lines which should not be deleted
			if (!currentLine.equals(lineOfItemToDelete)) {
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
