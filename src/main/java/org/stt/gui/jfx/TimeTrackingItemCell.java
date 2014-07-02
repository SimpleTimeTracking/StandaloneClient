package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

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
	private static String STYLE_NORMAL = "-fx-background-color: null;";
	private static String STYLE_START_OF_DAY = "-fx-background-color: lightblue;";

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

	private final ImageView runningImageView;

	private final Set<TimeTrackingItem> firstItemOfDay;

	private TimeTrackingItemCell(Builder builder) {
		this.editButton = checkNotNull(builder.editButton);
		this.continueButton = checkNotNull(builder.continueButton);
		this.deleteButton = checkNotNull(builder.deleteButton);
		this.fromToImageView = checkNotNull(builder.fromToImageView);
		this.runningImageView = checkNotNull(builder.runningImageView);
		this.firstItemOfDay = checkNotNull(builder.firstItemOfDay);

		final ContinueActionHandler continueActionHandler = checkNotNull(builder.continueActionHandler);
		final EditActionHandler editActionHandler = checkNotNull(builder.editActionHandler);
		final DeleteActionHandler deleteActionHandler = checkNotNull(builder.deleteActionHandler);

		setupListenersForCallbacks(continueActionHandler, editActionHandler,
				deleteActionHandler);

		actionsPane.getChildren().addAll(deleteButton, continueButton,
				editButton);

		HBox.setHgrow(space, Priority.ALWAYS);
		labelForComment.setWrapText(true);
		labelForComment.setPrefWidth(400);

		timePane.setPrefWidth(200);
		timePane.setSpacing(5);
		timePane.setAlignment(Pos.CENTER_LEFT);

		cellPane.getChildren().addAll(actionsPane, labelForComment, space,
				timePane);
		cellPane.setAlignment(Pos.CENTER_LEFT);
		actionsPane.setAlignment(Pos.CENTER_LEFT);
	}

	private void setupListenersForCallbacks(
			final ContinueActionHandler continueActionHandler,
			final EditActionHandler editActionHandler,
			final DeleteActionHandler deleteActionHandler) {
		continueButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				continueActionHandler.continueItem(item);
			}
		});

		editButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				editActionHandler.edit(item);
			}
		});

		deleteButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				deleteActionHandler.delete(item);
			}
		});
	}

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		cellPane.setStyle(STYLE_NORMAL);
		if (empty) {
			this.item = null;
			setGraphic(null);
		} else {
			this.item = item;
			applyLabelForComment();
			setupTimePane();
			setGraphic(cellPane);
			if (firstItemOfDay.contains(item)) {
				cellPane.setStyle(STYLE_START_OF_DAY);
			}
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
			timePane.getChildren().setAll(labelForStart, runningImageView);
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

	public static class Builder {
		private Button editButton;
		private Button continueButton;
		private Button deleteButton;
		private ImageView fromToImageView;
		private ImageView runningImageView;
		private ContinueActionHandler continueActionHandler;
		private DeleteActionHandler deleteActionHandler;
		private EditActionHandler editActionHandler;
		private Set<TimeTrackingItem> firstItemOfDay;

		public Builder editImage(Image editImage) {
			this.editButton = new ImageButton(editImage);
			return this;
		}

		public Builder continueImage(Image continueImage) {
			this.continueButton = new ImageButton(continueImage);
			return this;
		}

		public Builder deleteImage(Image deleteImage) {
			this.deleteButton = new ImageButton(deleteImage);
			return this;
		}

		public Builder fromToImage(Image fromToImage) {
			this.fromToImageView = new ImageView(fromToImage);
			return this;
		}

		public Builder runningImage(Image runningImage) {
			this.runningImageView = new ImageView(runningImage);
			return this;
		}

		public Builder continueActionHandler(ContinueActionHandler handler) {
			this.continueActionHandler = handler;
			return this;
		}

		public Builder deleteActionHandler(DeleteActionHandler handler) {
			this.deleteActionHandler = handler;
			return this;
		}

		public Builder editActionHandler(EditActionHandler handler) {
			this.editActionHandler = handler;
			return this;
		}

		public void firstItemOfDaySet(Set<TimeTrackingItem> firstItemOfDay) {
			this.firstItemOfDay = checkNotNull(firstItemOfDay);

		}

		public TimeTrackingItemCell build() {
			return new TimeTrackingItemCell(this);
		}

	}
}
