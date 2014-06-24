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
	private static final Pattern P_FIN_AT = Pattern.compile(
			"\\s*fin\\s*at\\s*(.+)$", Pattern.MULTILINE);
	private static final Pattern P_FROM_TO = Pattern.compile(
			"(.+)\\s+(?:from)?\\s+(.+)\\s+to\\s+(.+)$", Pattern.MULTILINE);

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

		Optional<TimeTrackingItem> finAt = tryToParseFinAt(command);

		if (COMMAND_FIN.equals(command)) {
			return endCurrentItem(DateTime.now());
		} else if (finAt != null) {
			// really checking for null here. See comment on tryToParseFinAt
			return finAt;
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
		endCurrentItem(DateTime.now());
	}

	@Override
	public Optional<TimeTrackingItem> endCurrentItem(DateTime endTime) {
		Optional<TimeTrackingItem> currentTimeTrackingitem = itemSearcher
				.getCurrentTimeTrackingitem();
		if (currentTimeTrackingitem.isPresent()) {
			TimeTrackingItem unfinisheditem = currentTimeTrackingitem.get();
			TimeTrackingItem nowFinishedItem = unfinisheditem.withEnd(endTime);
			try {
				itemWriter.replace(unfinisheditem, nowFinishedItem);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return Optional.of(nowFinishedItem);
		}
		return Optional.<TimeTrackingItem> absent();
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

	/**
	 * Tries to parse "fin at 22:00". If it cannot be parsed, an
	 * IllegalStateException is thrown. If parsing succeeds, the current item is
	 * finished and returned.
	 * 
	 * If the command is not recognized at all, <b>null</b> is returned to
	 * distinguish between "cannot parse at all" and "no current item present"
	 */
	private Optional<TimeTrackingItem> tryToParseFinAt(String command) {
		Matcher finAtMatcher = P_FIN_AT.matcher(command);
		if (finAtMatcher.matches()) {
			String time = finAtMatcher.group(1);
			DateTime parsedTime = parseHoursMinutesOptionalSeconds(time);

			if (parsedTime == null) {
				throw new IllegalStateException("cannot parse time \"" + time
						+ "\"");
			}
			return endCurrentItem(parsedTime);
		}
		return null;
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
			result = tryToParseFromTo(command);
		}
		if (result == null) {
			result = new TimeTrackingItem(command, DateTime.now());
		}
		return result;
	}

	private TimeTrackingItem tryToParseSince(String command) {
		Matcher matcher = P_SINCE.matcher(command);
		if (matcher.matches()) {
			DateTime todayWithTime = parseHoursMinutesOptionalSeconds(matcher
					.group(2));
			return new TimeTrackingItem(matcher.group(1), todayWithTime);
		}
		return null;
	}

	private TimeTrackingItem tryToParseFromTo(String command) {
		Matcher matcher = P_FROM_TO.matcher(command);
		if (matcher.matches()) {
			DateTime fromTime = parseHoursMinutesOptionalSeconds(matcher
					.group(2));
			// if(fromTime)
			DateTime toTime = parseHoursMinutesOptionalSeconds(matcher.group(3));
			return new TimeTrackingItem(matcher.group(1), fromTime, toTime);
		}
		return null;
	}

	private DateTime parseHoursMinutesOptionalSeconds(String timeString) {
		DateTime time = parseWithHoursMinutesAndSeconds(timeString);
		if (time == null) {
			time = parseWithHoursAndMinutes(timeString);
		}
		if (time == null) {
			return null;
		}
		DateTime today = DateTime.now().withTimeAtStartOfDay();
		DateTime todayWithTime = today.withMillisOfDay(time.getMillisOfDay());
		return todayWithTime;
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
