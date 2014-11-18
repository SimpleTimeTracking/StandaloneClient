package org.stt.fun;

import com.google.common.base.Preconditions;
import java.util.ResourceBundle;
import org.stt.model.TimeTrackingItem;

/**
 *
 * @author dante
 */
public class DaysTrackedAchievement extends Achievement {

	private int daysTracked;
	private TimeTrackingItem lastItem;
	private final ResourceBundle resourceBundle;
	private final int daysRequired;
	private boolean achieved;

	public DaysTrackedAchievement(ResourceBundle resourceBundle, int daysRequired) {
		this.resourceBundle = Preconditions.checkNotNull(resourceBundle);
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
		if (lastItem == null || !lastItem.getStart().withTimeAtStartOfDay().equals(read.getStart().withTimeAtStartOfDay())) {
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
		return "ascended_level" + daysRequired + ".png";
	}

	@Override
	public String getDescription() {
		return String.format(resourceBundle.getString("achievement.daysTracked"), daysRequired);
	}

}
