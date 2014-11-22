package org.stt.gui.jfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.binding.ListBinding;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.stt.gui.jfx.TimeTrackingItemCell.ContinueActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.DeleteActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.EditActionHandler;
import org.stt.model.TimeTrackingItem;

public class ResultViewConfigurer {

	public ResultViewConfigurer() {

	}

	public ObservableList<TimeTrackingItem> configure(
			Property<TimeTrackingItem> selectedItem,
			ObservableList<TimeTrackingItem> allItems,
			StringProperty filterProperty, boolean filterDuplicates,
			final Callback callback) {
		ObservableList<TimeTrackingItem> result = createResultsListBinding(
				allItems, filterProperty, filterDuplicates);
		whenSelectionChangesNotifyCallback(callback, selectedItem);
		return result;
	}

	private void whenSelectionChangesNotifyCallback(final Callback callback,
			final Property<TimeTrackingItem> selectedItem) {
		selectedItem.addListener(new ChangeListener<TimeTrackingItem>() {

			@Override
			public void changed(
					ObservableValue<? extends TimeTrackingItem> observable,
					TimeTrackingItem oldItem, TimeTrackingItem newItem) {
				if (newItem != null) {
					selectedItem.setValue(null);
					if (newItem.getComment().isPresent()) {
						String textToSet = newItem.getComment().get();
						callback.textOfSelectedItem(textToSet);
					}
				}

			}
		});
	}

	private ObservableList<TimeTrackingItem> createResultsListBinding(
			final ObservableList<TimeTrackingItem> allItems,
			final StringProperty filterProperty, final boolean filterDuplicates) {
		return new ListBinding<TimeTrackingItem>() {
			{
				bind(allItems, filterProperty);
			}

			@Override
			protected ObservableList<TimeTrackingItem> computeValue() {
				List<TimeTrackingItem> result = filterListBy(allItems,
						filterProperty, filterDuplicates);
				return FXCollections.observableList(result);
			}

		};
	}

	private List<TimeTrackingItem> filterListBy(
			final ObservableList<TimeTrackingItem> allItems,
			final StringProperty filterProperty, boolean filterDuplicates) {
		List<TimeTrackingItem> result;
		String filter = filterProperty.get().toLowerCase();
		if (filter.isEmpty()) {
			result = new ArrayList<>(allItems);
		} else {
			result = new ArrayList<TimeTrackingItem>();
			Set<String> itemsInResult = new HashSet<>();
			for (TimeTrackingItem item : allItems) {
				if (item.getComment().isPresent()) {
					if ((!filterDuplicates || !itemsInResult.contains(item
							.getComment().get()))
							&& matchesFilter(item, filter)) {
						result.add(item);
						itemsInResult.add(item.getComment().get());
					}
				}
			}
		}
		Collections.reverse(result);
		return result;
	}

	private boolean matchesFilter(TimeTrackingItem item, String filter) {
		if (item.getComment().isPresent()
				&& item.getComment().get().toLowerCase().contains(filter)) {
			return true;
		}
		return false;
	}

	public interface Callback extends DeleteActionHandler, EditActionHandler,
			ContinueActionHandler {

		void textOfSelectedItem(String text);
	}
}
