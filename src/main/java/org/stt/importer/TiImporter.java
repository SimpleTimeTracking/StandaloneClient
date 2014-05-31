package org.stt.importer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemImporter;

/**
 * Imports all time tracking records of an existing (modified) ti installation.
 * All elements of the given inputFile are read. Format of the file is $comment
 * $start to $end where $comment, $start, and $end do not contain white space
 */
public class TiImporter implements ItemImporter {

	private File inputFile;

	public TiImporter(File inputFile) {
		this.inputFile = inputFile;
	}

	@Override
	public Collection<TimeTrackingItem> read() throws IOException {

		Collection<TimeTrackingItem> result = new LinkedList<>();

		LineIterator lineIter = FileUtils.lineIterator(inputFile);
		try {

			while (lineIter.hasNext()) {
				String nextLine = lineIter.nextLine();
				// ignore empty lines or ones just containing whitespace
				if (!nextLine.trim().isEmpty()) {
					result.add(constructFrom(nextLine));
				}
			}

		} finally {
			LineIterator.closeQuietly(lineIter);
		}

		return result;
	}

	private TimeTrackingItem constructFrom(String string) {
		TimeTrackingItem resultItem = new TimeTrackingItem();

		String[] split = string.split("\\s");
		resultItem.setComment(split[0]);
		// start time is in [1]
		// end time is in [3]

		return resultItem;
	}

}
