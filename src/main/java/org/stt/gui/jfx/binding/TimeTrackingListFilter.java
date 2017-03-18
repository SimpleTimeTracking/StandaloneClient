package org.stt.gui.jfx.binding;

import javafx.beans.binding.ListBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.stt.Streams;
import org.stt.model.TimeTrackingItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeTrackingListFilter extends ListBinding<TimeTrackingItem> {

	private final ObservableList<TimeTrackingItem> allItems;
	private final ObservableValue<String> filterProperty;
	private final boolean filterDuplicates;

	public TimeTrackingListFilter(ObservableList<TimeTrackingItem> allItems,
								  ObservableValue<String> filterProperty, boolean filterDuplicates) {
		this.allItems = Objects.requireNonNull(allItems);
        this.filterProperty = Objects.requireNonNull(filterProperty);
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
		String filter = filterProperty.getValue().toLowerCase();
		if (filter.isEmpty()) {
			result = new ArrayList<>(allItems);
		} else {
            Stream<TimeTrackingItem> processingStream = allItems.stream()
                    .filter(item -> item.getActivity().toLowerCase().contains(filter));
            if (filterDuplicates) {
                processingStream = processingStream.filter(Streams.distinctByKey(TimeTrackingItem::getActivity));
            }
            result = processingStream.collect(Collectors.toList());
        }
        Collections.reverse(result);
        return result;
	}
}
