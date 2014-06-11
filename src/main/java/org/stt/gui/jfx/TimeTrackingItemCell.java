package org.stt.gui.jfx;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;

public class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {
	public TimeTrackingItemCell() {
	}

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		Label label = createLabel();
		setGraphic(label);
	}

	private Label createLabel() {
		TimeTrackingItem item = getItem();
		boolean empty = isEmpty();
		Label label = new Label();

		if (!empty) {
			StringBuilder itemText = new StringBuilder();
			DateTimeFormatter dateTimeFormatter = DateTimeFormat
					.shortDateTime();
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
		return label;
	}
}
