package org.stt;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class DateTimeHelper {

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

	public static boolean isOnSameDay(DateTime d1, DateTime d2) {
		if (d1 == null || d2 == null) {
			return false;
		}
		return d1.getYear() == d2.getYear()
				&& d1.getDayOfYear() == d2.getDayOfYear();
	}
}
