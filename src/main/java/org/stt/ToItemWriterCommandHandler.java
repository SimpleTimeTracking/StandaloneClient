package org.stt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemSearcher;
import org.stt.persistence.ItemWriter;

import com.google.common.base.Optional;

public class ToItemWriterCommandHandler implements CommandHandler {
	private static final Pattern P_MINS_AGO = Pattern.compile(
			"(.+)\\s+(\\d+)\\s?min(ute)?s? ago$", Pattern.MULTILINE);
	private static final Pattern P_SECS_AGO = Pattern.compile(
			"(.+)\\s+(\\d+)\\s?s(ec(ond)?s?)? ago$", Pattern.MULTILINE);
	private static final Pattern P_HOURS_AGO = Pattern.compile(
			"(.+)\\s+(\\d+)\\s?h(rs?|ours?)? ago$", Pattern.MULTILINE);

	private final ItemWriter itemWriter;
	private final ItemSearcher itemSearcher;

	public ToItemWriterCommandHandler(ItemWriter itemWriter,
			ItemSearcher itemSearcher) {
		this.itemWriter = checkNotNull(itemWriter);
		this.itemSearcher = checkNotNull(itemSearcher);
	}

	@Override
	public TimeTrackingItem executeCommand(String command) {
		checkNotNull(command);
		TimeTrackingItem parsedItem = parse(command);
		try {
			Optional<TimeTrackingItem> currentTimeTrackingitem = itemSearcher
					.getCurrentTimeTrackingitem();
			if (currentTimeTrackingitem.isPresent()) {
				TimeTrackingItem unfinisheditem = currentTimeTrackingitem.get();
				TimeTrackingItem nowFinishedItem = unfinisheditem
						.withEnd(parsedItem.getStart());
				itemWriter.replace(unfinisheditem, nowFinishedItem);
			}
			itemWriter.write(parsedItem);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
		return parsedItem;
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
			result = new TimeTrackingItem(command, DateTime.now());
		}
		return result;
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
