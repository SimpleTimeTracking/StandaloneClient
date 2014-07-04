package org.stt.stt.importer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

public class STTItemWriter implements ItemWriter {
	private final PrintWriter out;
	private final STTItemConverter converter = new STTItemConverter();

	public STTItemWriter(Writer out) {
		this.out = new PrintWriter(checkNotNull(out));
	}

	@Override
	public void write(TimeTrackingItem item) throws IOException {
		out.println(converter.timeTrackingItemToLine(item));
	}

	@Override
	public void close() throws IOException {
		out.close();
	}
}
