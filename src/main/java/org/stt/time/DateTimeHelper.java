package org.stt.time;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class DateTimeHelper {

	public static final DateTimeFormatter hmsDateFormat = DateTimeFormat
			.forPattern("HH:mm:ss");
	public static final DateTimeFormatter ymdhmsDateFormat = DateTimeFormat
			.forPattern("yyyy-MM-dd HH:mm:ss");
	public static final DateTimeFormatter mdhmsDateFormat = DateTimeFormat
			.forPattern("MM-dd HH:mm:ss");
	public static final DateTimeFormatter ymdDateFormat = DateTimeFormat
			.forPattern("yyyy-MM-dd");

	public static final PeriodFormatter hmsPeriodFormatter = new PeriodFormatterBuilder()
			.printZeroAlways().minimumPrintedDigits(2).appendHours()
			.appendSuffix("h").appendSeparator(":").appendMinutes()
			.appendSuffix("m").appendSeparator(":").appendSeconds()
			.appendSuffix("s").toFormatter();

	public static final PeriodFormatter FORMATTER_PERIOD_H_M_S = new PeriodFormatterBuilder()
			.printZeroAlways().appendHours()
			.appendSeparator(":").appendMinutes()
			.appendSeparator(":").appendSeconds().toFormatter();

	public static boolean isToday(DateTime date) {
		return isOnSameDay(date, DateTime.now());
	}

	public static boolean isOnSameDay(DateTime d1, DateTime d2) {
		if (d1 == null || d2 == null) {
			return false;
		}
		return d1.getYear() == d2.getYear()
				&& d1.getDayOfYear() == d2.getDayOfYear();
	}

	/**
	 * @return if source is between from and to (both inclusive)
	 */
	public static boolean isBetween(DateTime source, DateTime from, DateTime to) {
		return !source.isBefore(from) && !source.isAfter(to);
	}

	/**
	 * returns the formatted date in format "HH:mm:ss" if the given date is
	 * today, "yyyy-MM-dd HH:mm:ss" if the given date is not today
	 */
	public static String prettyPrintTime(DateTime date) {
		if (isOnSameDay(date, DateTime.now())) {
			return hmsDateFormat.print(date);
		} else {
			return ymdhmsDateFormat.print(date);
		}
	}

	/**
	 * returns the formatted date in format "yyyy-MM-dd"
	 */
	public static String prettyPrintDate(DateTime date) {
		return ymdDateFormat.print(date);
	}

	public static String prettyPrintDuration(Duration duration) {
		if (duration.isShorterThan(new Duration(0))) {
			// it is negative
			return "-"
					+ hmsPeriodFormatter.print(new Duration(duration
									.getMillis() * -1).toPeriod());
		} else {
			return " " + hmsPeriodFormatter.print(duration.toPeriod());
		}
	}

}
