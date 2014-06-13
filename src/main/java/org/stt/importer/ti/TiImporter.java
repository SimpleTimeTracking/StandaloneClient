package org.stt.importer.ti;

import java.io.Reader;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Imports all time tracking records of an existing (modified) ti installation.
 * All elements of the given inputFile are read. Format of the file has to be
 * "$comment $start to $end" where $comment, $start, and $end do not contain
 * white space
 */
public class TiImporter implements ItemReader {

	private static final Logger LOG = Logger.getLogger(TiImporter.class
			.getName());

	private final LineIterator lineIter;
	private final DateTimeFormatter dateFormat = DateTimeFormat
			.forPattern("yyyy-MM-dd_HH:mm:ss");

	public TiImporter(Reader input) {
		lineIter = IOUtils.lineIterator(input);
	}

	@Override
	public Optional<TimeTrackingItem> read() {
		while (lineIter.hasNext()) {
			String nextLine = lineIter.nextLine();
			// ignore empty lines or ones just containing whitespace
			if (!nextLine.trim().isEmpty()) {
				try {
					return Optional.of(constructFrom(nextLine));
				} catch (ParseException e) {
					LOG.log(Level.SEVERE, "cannot parse line \"" + nextLine
							+ "\"", e);
				}
			}
		}
		lineIter.close();
		return Optional.absent();
	}

	private TimeTrackingItem constructFrom(String singleLine)
			throws ParseException {

		String[] splitLine = singleLine.split("\\s");
		Preconditions.checkState(splitLine.length == 4 || splitLine.length == 2, "The given line \""
				+ singleLine
				+ "\" must contain exactly 2 or 4 white space separated elements.");

		String comment = splitLine[0];
		comment = comment.replaceAll("_", " ");

		DateTime start = dateFormat.parseDateTime(splitLine[1]);
		if(splitLine.length > 2) {
			DateTime end = dateFormat.parseDateTime(splitLine[3]);
	
			return new TimeTrackingItem(comment, start, end);
		}
		
		return new TimeTrackingItem(comment, start);
	}

	@Override
	public void close() {
		lineIter.close();
	}

}
