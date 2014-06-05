package org.stt.importer.ti;

import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

/**
 * Imports all time tracking records written by {@link DefaultItemExporter}
 */
public class DefaultItemImporter implements ItemReader {

	private final LineIterator lineIter;
	// not thread safe but this should not matter here as it does not seem
	// reasonable to read with multiple threads here
	private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

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

		Calendar start = Calendar.getInstance();
		start.setTime(df.parse(splitLine[0]));

		Calendar end = null;
		if( splitLine.length > 1) {
			end = Calendar.getInstance();
			end.setTime(df.parse(splitLine[1]));
		}
		
		StringBuilder commentBuilder = new StringBuilder(singleLine.length());
		for (int i = 2; i < splitLine.length; i++) {
			String current = splitLine[i];
			current = current.replaceAll("\\\\r", "\r");
			current = current.replaceAll("\\\\n", "\n");
			commentBuilder.append(current);
			if(i < splitLine.length - 1) {
				commentBuilder.append(" ");
			}
		}

		if(end != null) {
			return new TimeTrackingItem(commentBuilder.toString(), start, end);
		}
		else {
			return new TimeTrackingItem(commentBuilder.toString(), start);
		}
	}

	@Override
	public void close() {
		lineIter.close();
	}

}
