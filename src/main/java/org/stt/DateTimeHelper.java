package org.stt;

import org.joda.time.DateTime;

public class DateTimeHelper {

	public static boolean isOnSameDay(DateTime d1, DateTime d2) {
		if (d1 == null || d2 == null) {
			return false;
		}
		return d1.getYear() == d2.getYear()
				&& d1.getDayOfYear() == d2.getDayOfYear();
	}
}
