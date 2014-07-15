package org.stt.cli;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.DateTimeHelper;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

public class ItemFormattingHelper {

	public static StringBuilder prettyPrintItem(
			Optional<TimeTrackingItem> optionalItem) {

		StringBuilder builder = new StringBuilder();

		if (optionalItem.isPresent()) {
			TimeTrackingItem item = optionalItem.get();
			DateTime start = item.getStart();
			DateTime end = item.getEnd().orNull();
			String comment = item.getComment().orNull();

			if (DateTimeHelper.isToday(start)) {
				builder.append(DateTimeHelper.hmsDateFormat.print(start));
			} else {
				builder.append(DateTimeHelper.mdhmsDateFormat.print(start));
			}
			builder.append(" - ");
			if (end == null) {
				builder.append("now     ");
			} else {
				builder.append(DateTimeHelper.hmsDateFormat.print(end));
			}
			builder.append(" ( ");
			builder.append(DateTimeHelper.hmsPeriodFormatter
					.print(new Duration(start, (end == null ? DateTime.now()
							: end)).toPeriod()));
			builder.append(" ) ");
			builder.append(" => ");
			builder.append(comment);
		}
		return builder;
	}
}
