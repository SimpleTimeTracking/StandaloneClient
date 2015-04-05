package org.stt.persistence.stt;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.io.Reader;

/**
 * Imports all time tracking records written by {@link STTItemPersister}
 */
public class STTItemReader implements ItemReader {

	private final LineIterator lineIter;

	private final STTItemConverter converter = new STTItemConverter();

	@Inject
	public STTItemReader(@STTFile Reader input) {
		lineIter = IOUtils.lineIterator(input);
	}

	@Override
	public Optional<TimeTrackingItem> read() {
		while (lineIter.hasNext()) {
			String nextLine = lineIter.nextLine();
			// ignore empty lines or ones just containing whitespace
			if (!nextLine.trim().isEmpty()) {
				return Optional.of(converter.lineToTimeTrackingItem(nextLine));
			}
		}
		lineIter.close();
		return Optional.absent();
	}

	@Override
	public void close() {
		lineIter.close();
	}
}
