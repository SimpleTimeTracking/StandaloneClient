package org.stt.fun;

import org.stt.model.TimeTrackingItem;

import java.time.Duration;
import java.util.ResourceBundle;

/**
 * Achieved when the sum of tracked (and ended) items is >= threshold.
 *
 * @author dante
 */
class HoursTrackedAchievement extends LocalizedAchievement {

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
		achieved = false;
	}

	@Override
	void process(TimeTrackingItem read) {
        read.getEnd().ifPresent(endTime -> {
            Duration duration = Duration.between(read.getStart(), endTime);
            timeTracked = timeTracked.plus(duration);
        });
    }

	@Override
	void done() {
        achieved = timeTracked.toHours() >= thresholdHours;
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
