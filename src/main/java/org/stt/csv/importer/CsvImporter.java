package org.stt.csv.importer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.time.DateTimes;

import java.io.Reader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Imports from .csv files
 */
public class CsvImporter implements ItemReader {

	private static final Logger LOG = Logger.getLogger(CsvImporter.class
			.getName());

	private final LineIterator lineIter;

    private final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("dd.MM.yyy");
    private final DateTimeFormatter durationParser = DateTimeFormatter
            .ofPattern("hh:mm");

	private int commentfieldIndex;
	private int datefieldIndex;
	private int timefieldIndex;

    private LocalDateTime nextStartTime = null;

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
        return Optional.empty();
    }

    public TimeTrackingItem constructFrom(String line, LocalDateTime startTime) {

		// we want all items, even empty ones: negative parameter to split does
		// exactly that
		String[] split = line.split(";", -1);
		if (split.length > (Math.max(
				Math.max(commentfieldIndex, datefieldIndex), timefieldIndex))) {
			String dateString = split[datefieldIndex].replaceAll("\"", "");
			String durationString = split[timefieldIndex].replaceAll("\"", "");
			String comment = split[commentfieldIndex];
			try {
                LocalDateTime parsedDateTime = LocalDateTime.parse(dateString, formatter);
                Duration period = Duration.between(LocalTime.MIDNIGHT, LocalTime.parse(durationString, durationParser));
                LocalDateTime itemStartTime = startTime;
                if (!DateTimes.isOnSameDay(startTime, parsedDateTime)) {
                    itemStartTime = parsedDateTime.toLocalDate().atStartOfDay();
                }
                LocalDateTime itemEndTime = itemStartTime.plus(period);

                return new TimeTrackingItem(comment,
                        itemStartTime, itemEndTime);
			} catch (IllegalArgumentException i) {
                LOG.log(Level.INFO, "not parseable line: " + line, i);
            }
		} else {
            LOG.info(() -> "not parseable line: " + line);
        }
		return null;
	}

	@Override
    public void close() {
        lineIter.close();
	}
}
