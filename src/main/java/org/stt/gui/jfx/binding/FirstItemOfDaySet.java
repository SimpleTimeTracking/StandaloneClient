package org.stt.gui.jfx.binding;

import com.google.common.base.Preconditions;
import javafx.beans.binding.SetBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import org.stt.model.TimeTrackingItem;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class FirstItemOfDaySet extends SetBinding<TimeTrackingItem> {

	private final ObservableList<TimeTrackingItem> allItems;

	public FirstItemOfDaySet(ObservableList<TimeTrackingItem> allItems) {
		this.allItems = Preconditions.checkNotNull(allItems);
		bind(allItems);
	}

	@Override
	protected ObservableSet<TimeTrackingItem> computeValue() {
		Set<TimeTrackingItem> result = createSetOfFirstItemOfTheDay();
		return FXCollections.observableSet(result);
	}

	private Set<TimeTrackingItem> createSetOfFirstItemOfTheDay() {
		SortedSet<TimeTrackingItem> items = new TreeSet<>(
				TimeTrackingItem.BY_START_COMPARATOR);
		items.addAll(allItems);
		TimeTrackingItem lastItem = null;
		for (Iterator<TimeTrackingItem> it = items.iterator(); it.hasNext();) {
			TimeTrackingItem item = it.next();
			if (lastItem != null
					&& lastItem.getStart().withTimeAtStartOfDay()
							.equals(item.getStart().withTimeAtStartOfDay())) {
				it.remove();
			}
			lastItem = item;
		}
		return items;
	}
}
