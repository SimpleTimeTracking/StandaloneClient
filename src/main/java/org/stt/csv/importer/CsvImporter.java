package org.stt.csv.importer;

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.time.DateTimeHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

/**
 * Imports from .csv files
 */
public class CsvImporter implements ItemReader {

	private static final Logger LOG = Logger.getLogger(CsvImporter.class
			.getName());

	private final LineIterator lineIter;

	private final DateTimeFormatter formatter = DateTimeFormat
			.forPattern("dd.MM.yyy");
	private final PeriodFormatter durationParser = new PeriodFormatterBuilder()
			.minimumPrintedDigits(2).printZeroAlways().appendHours()
			.appendSeparator(":").appendMinutes().toFormatter();

	private int commentfieldIndex;
	private int datefieldIndex;
	private int timefieldIndex;

	private DateTime nextStartTime = null;

	public CsvImporter(Reader input, int datefieldIndex, int timefieldIndex,
			int commentfieldIndex) {
		this.datefieldIndex = datefieldIndex;
		this.timefieldIndex = timefieldIndex;
		this.commentfieldIndex = commentfieldIndex;
		lineIter = IOUtils.lineIterator(input);
	}

	@Override
	public Optional<TimeTrackingItem> read() {

		while (lineIter.hasNext()) {
			String nextLine = lineIter.nextLine();
			// ignore empty lines or ones just containing whitespace
			if (!nextLine.trim().isEmpty()) {
				TimeTrackingItem constructedItem = constructFrom(nextLine,
						nextStartTime);
				if (constructedItem != null) {
					nextStartTime = constructedItem.getEnd().get();
					return Optional.of(constructedItem);
				}
			}
		}
		lineIter.close();
		return Optional.absent();
	}

	public TimeTrackingItem constructFrom(String line, DateTime startTime) {

		// we want all items, even empty ones: negative parameter to split does
		// exactly that
		String[] split = line.split(";", -1);
		if (split.length > (Math.max(
				Math.max(commentfieldIndex, datefieldIndex), timefieldIndex))) {
			String dateString = split[datefieldIndex].replaceAll("\"", "");
			String durationString = split[timefieldIndex].replaceAll("\"", "");
			String comment = split[commentfieldIndex];
			try {
				DateTime parsedDateTime = formatter.parseDateTime(dateString);
				Period period = durationParser.parsePeriod(durationString);
				DateTime itemStartTime = startTime;
				if (!DateTimeHelper.isOnSameDay(startTime, parsedDateTime)) {
					itemStartTime = parsedDateTime.withTimeAtStartOfDay();
				}
				DateTime itemEndTime = itemStartTime.plus(period);
				TimeTrackingItem theItem = new TimeTrackingItem(comment,
						itemStartTime, itemEndTime);

				return theItem;
			} catch (IllegalArgumentException i) {
				LOG.info("not parseable line: " + line);
			}
		} else {
			LOG.info("not parseable line: " + line);
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		lineIter.close();
	}
}
