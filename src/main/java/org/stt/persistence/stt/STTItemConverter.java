package org.stt.persistence.stt;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;

class STTItemConverter {
	private final DateTimeFormatter dateFormat = DateTimeFormat
			.forPattern("yyyy-MM-dd_HH:mm:ss");

	public TimeTrackingItem lineToTimeTrackingItem(String singleLine) {

		List<String> splitLine = new LinkedList<>(Arrays.asList(singleLine
				.split(" ")));

		DateTime start = dateFormat.parseDateTime(splitLine.remove(0));

		DateTime end = null;
		if (splitLine.size() > 0) {
			try {
				end = dateFormat.parseDateTime(splitLine.get(0));
				splitLine.remove(0);
			} catch (IllegalArgumentException i) { // NOPMD
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

	public String timeTrackingItemToLine(TimeTrackingItem item) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(item.getStart().toString(dateFormat));
		builder.append(' ');
		if (item.getEnd().isPresent()) {
			builder.append(item.getEnd().get().toString(dateFormat));
			builder.append(' ');
		}

		if (item.getComment().isPresent()) {
			String oneLineComment = item.getComment().get();
			oneLineComment = oneLineComment.replaceAll("\r", "\\\\r");
			oneLineComment = oneLineComment.replaceAll("\n", "\\\\n");
			builder.append(oneLineComment);
		}

		return builder.toString();
	}
}
