package org.stt.persistence.stt;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import java.io.*;

/**
 * Writes {@link TimeTrackingItem}s to a new line. Multiline comments get joined
 * into one line: line endings \r and \n get replaced by the string \r and \n
 * respectively.
 * 
 */
@Singleton
public class STTItemPersister implements ItemPersister {

	private final STTItemConverter converter = new STTItemConverter();
	private Provider<Reader> readerProvider;
	private Provider<Writer> writerProvider;

	@Inject
	public STTItemPersister(@STTFile Provider<Reader> readerProvider, @STTFile Provider<Writer> writerProvider) {

		this.readerProvider = Preconditions.checkNotNull(readerProvider);
		this.writerProvider = Preconditions.checkNotNull(writerProvider);
	}

	@Override
	public void insert(TimeTrackingItem itemToInsert) throws IOException {
		Preconditions.checkNotNull(itemToInsert);
		StringWriter stringWriter = new StringWriter();
		Reader providedReader;
		providedReader = readerProvider.get();
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

		try (BufferedReader reader = new BufferedReader(readerProvider.get())) {
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

			printWriter.flush();
			rewriteFileWith(stringWriter.toString());
		}
	}

	private void rewriteFileWith(String content) throws IOException {
		Writer truncatingWriter = writerProvider.get();
		truncatingWriter.write(content);
		truncatingWriter.close();
	}

	@Override
	public void close() {
	}
}
