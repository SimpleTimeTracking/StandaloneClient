package org.stt.gui.jfx;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;

public class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {
	private final Label label = new Label();

	public TimeTrackingItemCell() {
	}

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		if (!empty) {
			StringBuilder itemText = new StringBuilder();
			DateTimeFormatter dateTimeFormatter = DateTimeFormat.shortDate();
			itemText.append(item.getStart().toString(dateTimeFormatter));
			if (item.getEnd().isPresent()) {
				itemText.append(" - ");
				itemText.append(item.getEnd().get().toString(dateTimeFormatter));
			}
			if (item.getComment().isPresent()) {
				itemText.append(": ");
				itemText.append(item.getComment().get());
			}

			label.setText(itemText.toString());

		} else {
			label.setText("");
		}
		setGraphic(label);
	}
}
