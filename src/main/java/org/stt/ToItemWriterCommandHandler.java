package org.stt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;
import org.stt.searching.ItemSearcher;

import com.google.common.base.Optional;

public class ToItemWriterCommandHandler implements CommandHandler {
	public static final String COMMAND_FIN = "fin";

	private static final Pattern P_MINS_AGO = Pattern.compile(
			"(.+)\\s+(\\d+)\\s?min(ute)?s? ago$", Pattern.MULTILINE);
	private static final Pattern P_SECS_AGO = Pattern.compile(
			"(.+)\\s+(\\d+)\\s?s(ec(ond)?s?)? ago$", Pattern.MULTILINE);
	private static final Pattern P_HOURS_AGO = Pattern.compile(
			"(.+)\\s+(\\d+)\\s?h(rs?|ours?)? ago$", Pattern.MULTILINE);
	private static final Pattern P_SINCE = Pattern.compile(
			"(.+)\\s+since\\s+(.+)$", Pattern.MULTILINE);

	private final ItemWriter itemWriter;
	private final ItemSearcher itemSearcher;

	public ToItemWriterCommandHandler(ItemWriter itemWriter,
			ItemSearcher itemSearcher) {
		this.itemWriter = checkNotNull(itemWriter);
		this.itemSearcher = checkNotNull(itemSearcher);
	}

	@Override
	public Optional<TimeTrackingItem> executeCommand(String command) {
		checkNotNull(command);

		if (COMMAND_FIN.equals(command)) {
			try {
				return endCurrentItemIfPresent(DateTime.now());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		} else {
			TimeTrackingItem parsedItem = parse(command);
			try {
				itemWriter.write(parsedItem);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			return Optional.of(parsedItem);
		}
	}

	@Override
	public void endCurrentItem() {
		try {
			endCurrentItemIfPresent(DateTime.now());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void resumeGivenItem(TimeTrackingItem item) {
		TimeTrackingItem newItem = new TimeTrackingItem(
				item.getComment().get(), DateTime.now());
		try {
			itemWriter.write(newItem);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Optional<TimeTrackingItem> endCurrentItemIfPresent(
			DateTime startTimeOfNewItem) throws IOException {
		Optional<TimeTrackingItem> currentTimeTrackingitem = itemSearcher
				.getCurrentTimeTrackingitem();
		if (currentTimeTrackingitem.isPresent()) {
			TimeTrackingItem unfinisheditem = currentTimeTrackingitem.get();
			TimeTrackingItem nowFinishedItem = unfinisheditem
					.withEnd(startTimeOfNewItem);
			itemWriter.replace(unfinisheditem, nowFinishedItem);
			return Optional.of(nowFinishedItem);
		}
		return Optional.<TimeTrackingItem> absent();
	}

	private TimeTrackingItem parse(String command) {
		TimeTrackingItem result = tryToParseMinutes(command);
		if (result == null) {
			result = tryToParseSeconds(command);
		}
		if (result == null) {
			result = tryToParseHours(command);
		}
		if (result == null) {
			result = tryToParseSince(command);
		}
		if (result == null) {
			result = new TimeTrackingItem(command, DateTime.now());
		}
		return result;
	}

	private TimeTrackingItem tryToParseSince(String command) {
		Matcher matcher = P_SINCE.matcher(command);
		if (matcher.matches()) {
			DateTime time = parseWithHoursMinutesAndSeconds(matcher.group(2));
			if (time == null) {
				time = parseWithHoursAndMinutes(matcher.group(2));
			}
			DateTime today = DateTime.now().withTimeAtStartOfDay();
			DateTime todayWithTime = today.withMillisOfDay(time
					.getMillisOfDay());
			return new TimeTrackingItem(matcher.group(1), todayWithTime);
		}
		return null;
	}

	private DateTime parseWithHoursAndMinutes(String time) {
		return parseTimeWithPattern(time, "kk:mm");
	}

	private DateTime parseWithHoursMinutesAndSeconds(String time) {
		return parseTimeWithPattern(time, "kk:mm:ss");
	}

	private DateTime parseTimeWithPattern(String time, String pattern) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
		return parseTimeWithFormatterOrReturnNull(time, formatter);
	}

	private DateTime parseTimeWithFormatterOrReturnNull(String time,
			DateTimeFormatter formatter) {
		try {
			return formatter.parseDateTime(time);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private TimeTrackingItem tryToParseSeconds(String command) {
		Matcher matcher = P_SECS_AGO.matcher(command);
		if (matcher.matches()) {
			int seconds = Integer.parseInt(matcher.group(2));
			return new TimeTrackingItem(matcher.group(1), DateTime.now()
					.minusSeconds(seconds));
		}
		return null;
	}

	private TimeTrackingItem tryToParseMinutes(String command) {
		Matcher matcher = P_MINS_AGO.matcher(command);
		if (matcher.matches()) {
			final int minutes = Integer.parseInt(matcher.group(2));
			return new TimeTrackingItem(matcher.group(1), DateTime.now()
					.minusMinutes(minutes));
		}
		return null;
	}

	private TimeTrackingItem tryToParseHours(String command) {
		Matcher matcher = P_HOURS_AGO.matcher(command);
		if (matcher.matches()) {
			final int hours = Integer.parseInt(matcher.group(2));
			return new TimeTrackingItem(matcher.group(1), DateTime.now()
					.minusHours(hours));
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		itemWriter.close();
	}
}
