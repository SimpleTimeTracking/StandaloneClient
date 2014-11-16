package org.stt.time;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 *
 * @author dante
 */
public class Formats {

	public static final PeriodFormatter FORMATTER_PERIOD_HH_MM_SS = new PeriodFormatterBuilder()
			.printZeroAlways().appendHours()
			.appendSeparator(":").appendMinutes()
			.appendSeparator(":").appendSeconds().toFormatter();

}
