package org.stt.cli;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.model.TimeTrackingItem;
import org.stt.time.DateTimeHelper;

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
				builder.append(DateTimeHelper.DATE_TIME_FORMATTER_HH_MM_SS.print(start));
			} else {
				builder.append(DateTimeHelper.DATE_TIME_FORMATTER_MM_DD_HH_MM_SS.print(start));
			}
			builder.append(" - ");
			if (end == null) {
				builder.append("now     ");
			} else {
				builder.append(DateTimeHelper.DATE_TIME_FORMATTER_HH_MM_SS.print(end));
			}
			builder.append(" ( ");
			builder.append(DateTimeHelper.FORMATTER_PERIOD_HHh_MMm_SSs
					.print(new Duration(start, (end == null ? DateTime.now()
							: end)).toPeriod()));
			builder.append(" ) ");
			builder.append(" => ");
			builder.append(comment);
		}
		return builder;
	}
}
