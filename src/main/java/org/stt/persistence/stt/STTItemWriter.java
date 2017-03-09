package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Objects;

public class STTItemWriter implements ItemWriter {
	private final PrintWriter out;
	private final STTItemConverter converter = new STTItemConverter();

	@Inject
	public STTItemWriter(@STTFile Writer out) {
        this.out = new PrintWriter(Objects.requireNonNull(out));
    }

	@Override
    public void write(TimeTrackingItem item) {
        out.println(converter.timeTrackingItemToLine(item));
    }

	@Override
    public void close() {
        out.close();
    }
}
