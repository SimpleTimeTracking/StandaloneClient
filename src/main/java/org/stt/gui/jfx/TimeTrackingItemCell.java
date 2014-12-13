package org.stt.gui.jfx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;
import org.stt.model.TimeTrackingItemFilter;

import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {

	private final HBox cellPane = new HBox(10);

	private final Label labelForComment = new Label();

	private final HBox timePane = new HBox();

	private final Label labelForStart = new Label();

	private final Label labelForEnd = new Label();

	final Button editButton;

	final Button continueButton;

	final Button deleteButton;

	private final ImageView fromToImageView;

	private final ImageView runningImageView;

	private final TimeTrackingItemFilter firstItemOfTheDayFilter;
	private final BorderPane firstDayPane;

	private final Separator separator;

	public TimeTrackingItemCell(Builder builder) {
		ResourceBundle localization = builder.resourceBundle;
		this.editButton = new ImageButton(checkNotNull(builder.editImage));
		this.continueButton = new ImageButton(
				checkNotNull(builder.continueImage));
		this.deleteButton = new ImageButton(checkNotNull(builder.deleteImage));
		setupTooltips(localization);
		this.fromToImageView = new ImageView(checkNotNull(builder.fromToImage));
		this.runningImageView = new ImageView(
				checkNotNull(builder.runningImage));
		this.firstItemOfTheDayFilter = checkNotNull(builder.firstItemOfTheDayFilter);

		final ContinueActionHandler continueActionHandler = checkNotNull(builder.continueActionHandler);
		final EditActionHandler editActionHandler = checkNotNull(builder.editActionHandler);
		final DeleteActionHandler deleteActionHandler = checkNotNull(builder.deleteActionHandler);

		setupListenersForCallbacks(continueActionHandler, editActionHandler,
				deleteActionHandler);

		HBox actionsPane = new HBox();
		actionsPane.getChildren().addAll(deleteButton, continueButton,
				editButton);

		Pane space = new Pane();
		HBox.setHgrow(space, Priority.ALWAYS);
		labelForComment.setWrapText(true);
		labelForComment.setPrefWidth(350);

		timePane.setPrefWidth(250);
		timePane.setSpacing(5);
		timePane.setAlignment(Pos.CENTER_LEFT);

		cellPane.getChildren().addAll(actionsPane, labelForComment, space,
				timePane);
		cellPane.setAlignment(Pos.CENTER_LEFT);
		actionsPane.setAlignment(Pos.CENTER_LEFT);

		firstDayPane = new BorderPane();
		separator = new Separator();

		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
	}

	protected void setupTooltips(ResourceBundle localization) {
		editButton.setTooltip(new Tooltip(localization
				.getString("itemList.edit")));
		continueButton.setTooltip(new Tooltip(localization
				.getString("itemList.continue")));
		deleteButton.setTooltip(new Tooltip(localization
				.getString("itemList.delete")));
	}

	private void setupListenersForCallbacks(
			final ContinueActionHandler continueActionHandler,
			final EditActionHandler editActionHandler,
			final DeleteActionHandler deleteActionHandler) {
		continueButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				continueActionHandler.continueItem(getItem());
			}
		});

		editButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				editActionHandler.edit(getItem());
			}
		});

		deleteButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				deleteActionHandler.delete(getItem());
			}
		});
	}

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
			applyLabelForComment();
			setupTimePane();
			if (firstItemOfTheDayFilter.filter(item)) {
				setupFirstDayPane();
				setGraphic(firstDayPane);
			} else {
				setupCellPane();
				setGraphic(cellPane);
			}
		}
	}

	private void setupCellPane() {
		firstDayPane.setCenter(null);
		firstDayPane.setBottom(null);
	}

	private void setupFirstDayPane() {
		firstDayPane.setCenter(cellPane);
		firstDayPane.setBottom(separator);
	}

	private void applyLabelForComment() {
		if (getItem().getComment().isPresent()) {
			labelForComment.setText(getItem().getComment().get());
		} else {
			labelForComment.setText("");
		}
	}

	private void setupTimePane() {
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.shortDateTime();
		labelForStart.setText(dateTimeFormatter.print(getItem().getStart()));

		if (!getItem().getEnd().isPresent()) {
			timePane.getChildren().setAll(labelForStart, runningImageView);
		} else {
			labelForEnd.setText(dateTimeFormatter.print(getItem().getEnd()
					.get()));
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

		private ContinueActionHandler continueActionHandler;
		private DeleteActionHandler deleteActionHandler;
		private EditActionHandler editActionHandler;
		private TimeTrackingItemFilter firstItemOfTheDayFilter;
		private ResourceBundle resourceBundle;
		private Image editImage;
		private Image deleteImage;
		private Image continueImage;
		private Image fromToImage;
		private Image runningImage;

		public Builder editImage(Image editImage) {
			this.editImage = checkNotNull(editImage);
			return this;
		}

		public Builder continueImage(Image continueImage) {
			this.continueImage = checkNotNull(continueImage);
			return this;
		}

		public Builder deleteImage(Image deleteImage) {
			this.deleteImage = checkNotNull(deleteImage);
			return this;
		}

		public Builder fromToImage(Image fromToImage) {
			this.fromToImage = checkNotNull(fromToImage);
			return this;
		}

		public Builder runningImage(Image runningImage) {
			this.runningImage = checkNotNull(runningImage);
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

		public Builder firstItemOfTheDayFilter(
				TimeTrackingItemFilter firstItemOfTheDayFilter) {
			this.firstItemOfTheDayFilter = checkNotNull(firstItemOfTheDayFilter);
			return this;
		}

		public Builder resourceBundle(ResourceBundle resourceBundle) {
			this.resourceBundle = checkNotNull(resourceBundle);
			return this;
		}
	}
}
