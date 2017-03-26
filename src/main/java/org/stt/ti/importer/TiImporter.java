package org.stt.ti.importer;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.stt.States.requireThat;

/**
 * Imports all time tracking records of an existing (modified) ti installation.
 * All elements of the given inputFile are read. Format of the file has to be
 * "$comment $start to $end" where $comment, $start, and $end do not contain
 * white space
 */
public class TiImporter implements ItemReader {

    private final BufferedReader reader;
    private final DateTimeFormatter dateFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd_HH:mm:ss");

	public TiImporter(Reader input) {
        reader = new BufferedReader(input);
    }

	@Override
	public Optional<TimeTrackingItem> read() {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                // ignore empty lines or ones just containing whitespace
                if (!line.trim().isEmpty()) {
                    return Optional.of(constructFrom(line));

                }
            }
            reader.close();
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

	private TimeTrackingItem constructFrom(String singleLine) {

		String[] splitLine = singleLine.split("\\s");
        requireThat(
                splitLine.length == 4 || splitLine.length == 2,
						"The given line \""
								+ singleLine
								+ "\" must contain exactly 2 or 4 white space separated elements.");

		String comment = splitLine[0];
		comment = comment.replaceAll("_", " ");

        LocalDateTime start = LocalDateTime.parse(splitLine[1], dateFormat);
        if (splitLine.length > 2) {
            LocalDateTime end = LocalDateTime.parse(splitLine[3], dateFormat);

			return new TimeTrackingItem(comment, start, end);
		}

		return new TimeTrackingItem(comment, start);
	}

	@Override
	public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
