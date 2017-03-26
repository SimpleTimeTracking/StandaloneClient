package org.stt.time;


import javax.inject.Singleton;
import java.time.Duration;
import java.util.Objects;

@Singleton
public class DurationRounder {

	private long intervalMillis;
	private long tieBreakMillis;

	public void setInterval(Duration interval) {
        Objects.requireNonNull(interval);
        intervalMillis = interval.toMillis();
        tieBreakMillis = intervalMillis / 2;
	}

	public Duration roundDuration(Duration duration) {
        Objects.requireNonNull(duration);

        long millisToRound = duration.toMillis();
        long segments = millisToRound / intervalMillis;
		long result = segments * intervalMillis;
		long delta = millisToRound - result;
		if (delta == tieBreakMillis && segments % 2 == 1 || delta > tieBreakMillis) {
			result += intervalMillis;
		}
        return Duration.ofMillis(result);
    }

}
