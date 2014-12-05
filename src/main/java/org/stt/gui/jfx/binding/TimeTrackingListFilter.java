package org.stt.gui.jfx.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.binding.ListBinding;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.stt.model.TimeTrackingItem;

import com.google.common.base.Preconditions;

public class TimeTrackingListFilter extends ListBinding<TimeTrackingItem> {

	private final ObservableList<TimeTrackingItem> allItems;
	private final StringProperty filterProperty;
	private final boolean filterDuplicates;

	public TimeTrackingListFilter(ObservableList<TimeTrackingItem> allItems,
			StringProperty filterProperty, boolean filterDuplicates) {
		this.allItems = Preconditions.checkNotNull(allItems);
		this.filterProperty = Preconditions.checkNotNull(filterProperty);
		this.filterDuplicates = filterDuplicates;

		bind(allItems, filterProperty);
	}

	@Override
	protected ObservableList<TimeTrackingItem> computeValue() {
		List<TimeTrackingItem> result = createFilteredList();
		return FXCollections.observableList(result);
	}

	private List<TimeTrackingItem> createFilteredList() {
		List<TimeTrackingItem> result;
		String filter = filterProperty.get().toLowerCase();
		if (filter.isEmpty()) {
			result = new ArrayList<>(allItems);
		} else {
			result = new ArrayList<TimeTrackingItem>();
			Set<String> itemsInResult = new HashSet<>();
			for (TimeTrackingItem item : allItems) {
				if (item.getComment().isPresent()
						&& (!filterDuplicates || !itemsInResult.contains(item
								.getComment().get()))
						&& matchesFilter(item, filter)) {
					result.add(item);
					itemsInResult.add(item.getComment().get());
				}
			}
		}
		Collections.reverse(result);
		return result;
	}

	private boolean matchesFilter(TimeTrackingItem item, String filter) {
		return item.getComment().isPresent()
				&& item.getComment().get().toLowerCase().contains(filter);
	}
}
