package org.stt.importer;

import java.io.IOException;
import java.io.Reader;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemImporter;

import com.google.common.base.Optional;

/**
 * Imports all time tracking records of an existing (modified) ti installation.
 * All elements of the given inputFile are read. Format of the file is $comment
 * $start to $end where $comment, $start, and $end do not contain white space
 */
public class TiImporter implements ItemImporter {

	private final LineIterator lineIter;

	public TiImporter(Reader input) {
		lineIter = IOUtils.lineIterator(input);
	}

	@Override
	public Optional<TimeTrackingItem> read() throws IOException {
		while (lineIter.hasNext()) {
			String nextLine = lineIter.nextLine();
			// ignore empty lines or ones just containing whitespace
			if (!nextLine.trim().isEmpty()) {
				return Optional.of(constructFrom(nextLine));
			}
		}
		lineIter.close();
		return Optional.absent();
	}

	private TimeTrackingItem constructFrom(String string) {
		String[] split = string.split("\\s");
		String comment = split[0];
		// start time is in [1]
		// end time is in [3]

		return new TimeTrackingItem(comment, Calendar.getInstance());
	}

	@Override
	public void close() {
		lineIter.close();
	}

}
