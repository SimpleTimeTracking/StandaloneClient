package org.stt.gui.jfx;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

import org.stt.model.TimeTrackingItem;

public class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {
	private final Label label = new Label();

	public TimeTrackingItemCell() {
	}

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		if (!empty && item.getComment().isPresent()) {
			label.setText(item.getComment().get());

		} else {
			label.setText("");
		}
		setGraphic(label);
	}
}
