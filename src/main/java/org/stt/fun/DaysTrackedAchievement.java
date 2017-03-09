package org.stt.fun;

import org.stt.model.TimeTrackingItem;

import java.util.ResourceBundle;

/**
 *
 * @author dante
 */
class DaysTrackedAchievement extends LocalizedAchievement {

	private int daysTracked;
	private TimeTrackingItem lastItem;
	private final int daysRequired;
	private boolean achieved;

	public DaysTrackedAchievement(ResourceBundle resourceBundle, int daysRequired) {
		super(resourceBundle);
		this.daysRequired = daysRequired;
	}

	@Override
	void start() {
		daysTracked = 0;
		lastItem = null;
		achieved = false;
	}

	@Override
	void done() {
		if (daysTracked >= daysRequired) {
			achieved = true;
		}
	}

	@Override
	void process(TimeTrackingItem read) {
        if (lastItem == null || !lastItem.getStart().toLocalDate().equals(read.getStart().toLocalDate())) {
            daysTracked++;
			lastItem = read;
		}
	}

	@Override
	boolean isAchieved() {
		return achieved;
	}

	@Override
	public String getCode() {
		return "daysTracked" + daysRequired;
	}

	@Override
	public String getDescription() {
		return String.format(localize("achievement.daysTracked"), daysRequired, daysRequired);
	}

}
