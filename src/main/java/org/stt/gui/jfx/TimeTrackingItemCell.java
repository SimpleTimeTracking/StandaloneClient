package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;

public class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {
	private final HBox cellPane = new HBox(10);

	private final HBox actionsPane = new HBox();

	private final Label labelForComment = new Label();

	private final Pane space = new Pane();

	private final HBox timePane = new HBox();

	private final Label labelForStart = new Label();

	private final Label labelForEnd = new Label();

	private TimeTrackingItem item;

	final Button editButton;

	final Button continueButton;

	final Button deleteButton;

	private final ImageView fromToImageView;

	public TimeTrackingItemCell(
			final ContinueActionHandler continueActionHandler,
			final EditActionHandler editActionHandler,
			final DeleteActionHandler deleteActionHandler,
			Image imageForContinue, Image imageForEdit, Image imageForDelete,
			Image imageFromTo) {
		checkNotNull(imageFromTo);
		checkNotNull(editActionHandler);
		checkNotNull(continueActionHandler);
		checkNotNull(deleteActionHandler);
		checkNotNull(imageForContinue);
		checkNotNull(imageForEdit);

		continueButton = new ImageButton(imageForContinue);
		continueButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				continueActionHandler.continueItem(item);
			}
		});

		editButton = new ImageButton(imageForEdit);
		editButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				editActionHandler.edit(item);
			}
		});

		deleteButton = new ImageButton(imageForDelete);
		deleteButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				deleteActionHandler.delete(item);
			}
		});

		actionsPane.getChildren().addAll(deleteButton, continueButton,
				editButton);

		HBox.setHgrow(space, Priority.ALWAYS);
		labelForComment.setWrapText(true);
		labelForComment.setPrefWidth(400);
		fromToImageView = new ImageView(imageFromTo);

		timePane.setPrefWidth(200);
		timePane.setSpacing(5);
		timePane.setAlignment(Pos.CENTER_LEFT);

		cellPane.getChildren().addAll(actionsPane, labelForComment, space,
				timePane);
		cellPane.setAlignment(Pos.CENTER_LEFT);
		actionsPane.setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			this.item = null;
			setGraphic(null);
		} else {
			this.item = item;
			applyLabelForComment();
			setupTimePane();
			setGraphic(cellPane);
		}
	}

	private void applyLabelForComment() {
		if (item.getComment().isPresent()) {
			labelForComment.setText(item.getComment().get());
		} else {
			labelForComment.setText("");
		}
	}

	private void setupTimePane() {
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.shortDateTime();
		labelForStart.setText(dateTimeFormatter.print(item.getStart()));

		if (!item.getEnd().isPresent()) {
			timePane.getChildren().setAll(labelForStart);
		} else {
			labelForEnd.setText(dateTimeFormatter.print(item.getEnd().get()));
			timePane.getChildren().setAll(labelForStart, fromToImageView,
					labelForEnd);
		}
	}

	public interface ContinueActionHandler {
		void continueItem(TimeTrackingItem item);
	}

	public interface EditActionHandler {
		void edit(TimeTrackingItem item);
	}

	public interface DeleteActionHandler {
		void delete(TimeTrackingItem item);
	}
}
