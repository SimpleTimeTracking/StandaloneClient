package org.stt.importer;

import java.io.Reader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

		List<String> splitLine = new LinkedList<>(Arrays.asList(singleLine
				.split(" ")));

		DateTime start = dateFormat.parseDateTime(splitLine.remove(0));

		DateTime end = null;
		if (splitLine.size() > 0) {
			try {
				end = dateFormat.parseDateTime(splitLine.get(0));
				splitLine.remove(0);
			} catch (IllegalArgumentException i) {
				// NOOP, if the string cannot be parsed, it is no date
				// this is a bit ugly but currently no idea how to do it
				// "correctly"
			}
		}
		String comment = null;
		if (splitLine.size() > 0) {
			StringBuilder commentBuilder = new StringBuilder(
					singleLine.length());
			for (String current : splitLine) {
				current = current.replaceAll("\\\\r", "\r");
				current = current.replaceAll("\\\\n", "\n");
				commentBuilder.append(current);

				commentBuilder.append(" ");
			}
			commentBuilder.deleteCharAt(commentBuilder.length() - 1);

			comment = commentBuilder.toString();
		}

		if (end != null) {
			return new TimeTrackingItem(comment, start, end);
		} else {
			return new TimeTrackingItem(comment, start);
		}
	}

	@Override
	public void close() {
		lineIter.close();
	}

}
