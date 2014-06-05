package org.stt.importer;

import java.io.Reader;
import java.text.ParseException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

/**
 * Imports all time tracking records written by {@link DefaultItemExporter}
 */
public class DefaultItemImporter implements ItemReader {

	private final LineIterator lineIter;

	private final DateTimeFormatter dateFormat = DateTimeFormat
			.forPattern("yyyy-MM-dd_HH:mm:ss");

	public DefaultItemImporter(Reader input) {
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
					// FIXME: we need logging and the possibility of generating
					// warning messages
					e.printStackTrace();
				}
			}
		}
		lineIter.close();
		return Optional.absent();
	}

	private TimeTrackingItem constructFrom(String singleLine)
			throws ParseException {

		String[] splitLine = singleLine.split(" ");

		DateTime start = dateFormat.parseDateTime(splitLine[0]);

		DateTime end = null;
		if (splitLine.length > 1) {
			end = dateFormat.parseDateTime(splitLine[1]);
		}

		StringBuilder commentBuilder = new StringBuilder(singleLine.length());
		for (int i = 2; i < splitLine.length; i++) {
			String current = splitLine[i];
			current = current.replaceAll("\\\\r", "\r");
			current = current.replaceAll("\\\\n", "\n");
			commentBuilder.append(current);
			if (i < splitLine.length - 1) {
				commentBuilder.append(" ");
			}
		}

		if (end != null) {
			return new TimeTrackingItem(commentBuilder.toString(), start, end);
		} else {
			return new TimeTrackingItem(commentBuilder.toString(), start);
		}
	}

	@Override
	public void close() {
		lineIter.close();
	}

}
