package org.stt.gui.jfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javafx.beans.binding.ListBinding;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;

import org.stt.gui.jfx.TimeTrackingItemCell.ContinueActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.DeleteActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.EditActionHandler;
import org.stt.model.TimeTrackingItem;
import org.stt.model.TimeTrackingItemFilter;

public class ResultViewConfigurer {
	public ResultViewConfigurer() {

	}

	public void configure(ListView<TimeTrackingItem> result,
			ObservableList<TimeTrackingItem> allItems,
			StringProperty filterProperty, final Callback callback) {
		result.setItems(createResultsListBinding(allItems, filterProperty));
		final MultipleSelectionModel<TimeTrackingItem> selectionModel = result
				.getSelectionModel();
		selectionModel.setSelectionMode(SelectionMode.SINGLE);
		setupCellFactory(result, allItems, callback);
		whenSelectionChangesNotifyCallback(callback, selectionModel);
	}

	private void whenSelectionChangesNotifyCallback(final Callback callback,
			final MultipleSelectionModel<TimeTrackingItem> selectionModel) {
		selectionModel.selectedItemProperty().addListener(
				new ChangeListener<TimeTrackingItem>() {

					@Override
					public void changed(
							ObservableValue<? extends TimeTrackingItem> observable,
							TimeTrackingItem oldItem, TimeTrackingItem newItem) {
						if (newItem != null) {
							selectionModel.clearSelection();
							if (newItem.getComment().isPresent()) {
								String textToSet = newItem.getComment().get();
								callback.textOfSelectedItem(textToSet);
							}
						}

					}
				});
	}

	private void setupCellFactory(ListView<TimeTrackingItem> result,
			ObservableList<TimeTrackingItem> allItems, final Callback callback) {
		final ObservableSet<TimeTrackingItem> firstItemOfDayBinding = createFirstItemOfDayBinding(allItems);
		result.setCellFactory(new TimeTrackingItemCellFactory(callback,
				callback, callback, new TimeTrackingItemFilter() {
					@Override
					public boolean filter(TimeTrackingItem item) {
						return firstItemOfDayBinding.contains(item);
					}
				}));
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

	private ObservableSet<TimeTrackingItem> createFirstItemOfDayBinding(
			final ObservableList<TimeTrackingItem> allItems) {
		return new SetBinding<TimeTrackingItem>() {
			{
				bind(allItems);
			}

			@Override
			protected ObservableSet<TimeTrackingItem> computeValue() {
				Set<TimeTrackingItem> result = getSetOfFirstItemOfTheDayIn(allItems);
				return FXCollections.observableSet(result);
			}

		};
	}

	private Set<TimeTrackingItem> getSetOfFirstItemOfTheDayIn(
			Collection<TimeTrackingItem> allItems) {
		SortedSet<TimeTrackingItem> result = new TreeSet<>(
				TimeTrackingItem.BY_START_COMPARATOR);
		result.addAll(allItems);
		TimeTrackingItem lastItem = null;
		for (Iterator<TimeTrackingItem> it = result.iterator(); it.hasNext();) {
			TimeTrackingItem item = it.next();
			if (lastItem != null
					&& lastItem.getStart().withTimeAtStartOfDay()
							.equals(item.getStart().withTimeAtStartOfDay())) {
				it.remove();
			}
			lastItem = item;
		}
		return result;
	}

	public interface Callback extends DeleteActionHandler, EditActionHandler,
			ContinueActionHandler {
		void textOfSelectedItem(String text);
	}
}
