package org.stt.fun;

import java.util.ResourceBundle;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.stt.model.TimeTrackingItem;

/**
 * Achieved, when the threshold amount of items was reached for at least one
 * day.
 *
 * @author dante
 */
public class AmountOfItemsAchievement extends LocalizedAchievement {

	private static final DateTimeComparator DATE_COMPARATOR = DateTimeComparator.getDateOnlyInstance();
	private final int amountOfItems;
	private DateTime currentDay;
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
		if (currentDay != null && DATE_COMPARATOR.compare(read.getStart(), currentDay) == 0) {
			currentDayItems++;
		} else {
			currentDayItems = 1;
			currentDay = read.getStart();
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
		return String.format(localize("achievement.twitchy"));
	}

}
