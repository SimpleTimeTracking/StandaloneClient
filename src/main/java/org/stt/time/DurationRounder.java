package org.stt.time;

import com.google.common.base.Preconditions;
import org.joda.time.Duration;

/**
 *
 * @author dante
 */
public class DurationRounder {

	private long intervalMillis;
	private long tieBreakMillis;

	public void setInterval(Duration interval) {
		Preconditions.checkNotNull(interval);
		intervalMillis = interval.getMillis();
		tieBreakMillis = intervalMillis / 2;
	}

	public Duration roundDuration(Duration duration) {
		Preconditions.checkNotNull(duration);

		long millisToRound = duration.getMillis();
		long segments = millisToRound / intervalMillis;
		long result = segments * intervalMillis;
		long delta = millisToRound - result;
		if (delta == tieBreakMillis && segments % 2 == 1 || delta > tieBreakMillis) {
			result += intervalMillis;
		}
		return Duration.millis(result);
	}

}
