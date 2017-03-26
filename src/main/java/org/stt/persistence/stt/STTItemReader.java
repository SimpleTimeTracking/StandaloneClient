package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Imports all time tracking records written by {@link STTItemPersister}
 */
public class STTItemReader implements ItemReader {

    private BufferedReader reader;

	private final STTItemConverter converter = new STTItemConverter();

    @Inject
    public STTItemReader(@STTFile Reader input) {

        reader = new BufferedReader(input);
    }

	@Override
	public Optional<TimeTrackingItem> read() {
        if (reader == null) {
            return Optional.empty();
        }
        String line;
        try {
            while (((line = reader.readLine()) != null)) {
                // ignore empty lines or ones just containing whitespace
                if (!line.trim().isEmpty()) {
                    return Optional.of(converter.lineToTimeTrackingItem(line));
                }
            }
            reader.close();
            reader = null;
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

	@Override
	public void close() {
        try {
            if (reader == null) {
                return;
            }
            reader.close();
            reader = null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
