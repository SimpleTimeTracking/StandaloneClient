package org.stt.fun;

import org.stt.model.TimeTrackingItem;

import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Achieved, when the threshold amount of items was reached for at least one
 * day.
 *
 * @author dante
 */
class AmountOfItemsAchievement extends LocalizedAchievement {

	private final int amountOfItems;
    private LocalDate currentDay;
    private int currentDayItems;
	private boolean achieved;

	public AmountOfItemsAchievement(ResourceBundle resourceBundle, int amountOfItems) {
		super(resourceBundle);
		this.amountOfItems = amountOfItems;
	}

	@Override
	void start() {
		currentDay = null;
		currentDayItems = 0;
		achieved = false;
	}

	@Override
	void process(TimeTrackingItem read) {
        LocalDate dayOfItem = read.getStart().toLocalDate();
        if (currentDay != null && currentDay.equals(dayOfItem)) {
            currentDayItems++;
		} else {
			currentDayItems = 1;
            currentDay = dayOfItem;
        }
		if (currentDayItems >= amountOfItems) {
			achieved = true;
		}
	}

	@Override
	boolean isAchieved() {
		return achieved;
	}

	@Override
	public String getCode() {
		return "twitchy" + amountOfItems;
	}

	@Override
	public String getDescription() {
        return localize("achievement.twitchy");
    }

}
