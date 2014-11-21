package org.stt.fun;

import java.util.ResourceBundle;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.model.TimeTrackingItem;

/**
 * Achieved when the sum of tracked (and ended) items is >= threshold.
 *
 * @author dante
 */
public class HoursTrackedAchievement extends LocalizedAchievement {

	private final int thresholdHours;
	private Duration timeTracked;
	private boolean achieved;

	public HoursTrackedAchievement(ResourceBundle resourceBundle, int thresholdHours) {
		super(resourceBundle);
		this.thresholdHours = thresholdHours;
	}

	@Override
	void start() {
		timeTracked = Duration.ZERO;
	}

	@Override
	void process(TimeTrackingItem read) {
		if (read.getEnd().isPresent()) {
			DateTime end = read.getEnd().get();
			Duration duration = new Duration(read.getStart(), end);
			timeTracked = timeTracked.plus(duration);
		}
	}

	@Override
	void done() {
		achieved = timeTracked.getStandardHours() >= thresholdHours;
	}

	@Override
	boolean isAchieved() {
		return achieved;
	}

	@Override
	public String getCode() {
		return "big_spender" + thresholdHours;
	}

	@Override
	public String getDescription() {
		return String.format(localize("achievement.bigSpender"), thresholdHours);
	}

}
