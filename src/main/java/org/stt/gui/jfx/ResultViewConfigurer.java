package org.stt.gui.jfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ListProperty;
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

	public void configure(ListProperty<TimeTrackingItem> result,
			Property<TimeTrackingItem> selectedItem,
			ObservableList<TimeTrackingItem> allItems,
			StringProperty filterProperty, final Callback callback) {
		result.set(createResultsListBinding(allItems, filterProperty));
		whenSelectionChangesNotifyCallback(callback, selectedItem);
	}

	private void whenSelectionChangesNotifyCallback(final Callback callback,
			final Property<TimeTrackingItem> selectedItem) {
		selectedItem.addListener(
				new ChangeListener<TimeTrackingItem>() {

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
			final StringProperty filterProperty) {
		return new ListBinding<TimeTrackingItem>() {
			{
				bind(allItems, filterProperty);
			}

			@Override
			protected ObservableList<TimeTrackingItem> computeValue() {
				List<TimeTrackingItem> result = filterListBy(allItems,
						filterProperty);
				return FXCollections.observableList(result);
			}

		};
	}

	private List<TimeTrackingItem> filterListBy(
			final ObservableList<TimeTrackingItem> allItems,
			final StringProperty filterProperty) {
		List<TimeTrackingItem> result;
		String filter = filterProperty.get().toLowerCase();
		if (filter.isEmpty()) {
			result = new ArrayList<>(allItems);
		} else {
			result = new ArrayList<TimeTrackingItem>();
			for (TimeTrackingItem item : allItems) {
				addToListIfItemMatchesFilter(result, filter, item);
			}
		}
		Collections.reverse(result);
		return result;
	}

	private void addToListIfItemMatchesFilter(List<TimeTrackingItem> result,
			String filter, TimeTrackingItem item) {
		if (item.getComment().isPresent()
				&& item.getComment().get().toLowerCase().contains(filter)) {
			result.add(item);
		}
	}

	public interface Callback extends DeleteActionHandler, EditActionHandler,
			ContinueActionHandler {

		void textOfSelectedItem(String text);
	}
}
