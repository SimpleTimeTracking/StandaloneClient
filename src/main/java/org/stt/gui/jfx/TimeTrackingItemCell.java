package org.stt.gui.jfx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;

public class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {
	private final STTApplication application;

	public TimeTrackingItemCell(STTApplication application) {
		this.application = application;
	}

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		Node graphic = createGraphic();
		setGraphic(graphic);
	}

	private Node createGraphic() {
		final TimeTrackingItem item = getItem();
		boolean empty = isEmpty();
		FlowPane pane = new FlowPane();
		if (!empty) {
			Label label = createLabel(item);

			Image image = new Image(getClass().getResource("/Continue.png")
					.toString(), 18, 18, true, true);
			Button btn = new ImageButton(image);
			btn.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					application.continueItem(item);
				}
			});
			pane.getChildren().addAll(btn, label);
		}
		return pane;
	}

	private Label createLabel(TimeTrackingItem item) {
		Label label = new Label();
		StringBuilder itemText = new StringBuilder();
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.shortDateTime();
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

		return label;
	}
}
