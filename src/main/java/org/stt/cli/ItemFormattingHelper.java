package org.stt.cli;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.DateTimeHelper;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

public class ItemFormattingHelper {

	public static final DateTimeFormatter hmsDateFormat = DateTimeFormat
			.forPattern("HH:mm:ss");
	public static final DateTimeFormatter mdhmsDateFormat = DateTimeFormat
			.forPattern("MM-dd HH:mm:ss");
	public static final DateTimeFormatter ymdDateFormat = DateTimeFormat
			.forPattern("yyyy-MM-dd");

	public static final PeriodFormatter hmsPeriodFormatter = new PeriodFormatterBuilder()
			.printZeroAlways().minimumPrintedDigits(2).appendHours()
			.appendSuffix("h").appendSeparator(":").appendMinutes()
			.appendSuffix("m").appendSeparator(":").appendSeconds()
			.appendSuffix("s").toFormatter();

	public static StringBuilder prettyPrintItem(
			Optional<TimeTrackingItem> optionalItem) {

		StringBuilder builder = new StringBuilder();

		if (optionalItem.isPresent()) {
			TimeTrackingItem item = optionalItem.get();
			DateTime start = item.getStart();
			DateTime end = item.getEnd().orNull();
			String comment = item.getComment().orNull();

			if (DateTimeHelper.isOnSameDay(start, DateTime.now())) {
				builder.append(hmsDateFormat.print(start));
			} else {
				builder.append(mdhmsDateFormat.print(start));
			}
			builder.append(" - ");
			if (end == null) {
				builder.append("now     ");
			} else {
				builder.append(hmsDateFormat.print(end));
			}
			builder.append(" ( ");
			builder.append(hmsPeriodFormatter.print(new Duration(start,
					(end == null ? DateTime.now() : end)).toPeriod()));
			builder.append(" ) ");
			builder.append(" => ");
			builder.append(comment);
		}
		return builder;
	}
}
