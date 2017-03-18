package org.stt.gui.jfx;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.stt.model.TimeTrackingItem;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static org.stt.gui.jfx.Glyph.glyph;

class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private static final int GLYPH_SIZE = 20;

    private final HBox cellPane = new HBox(2);
    private final Label labelForComment = new Label();
    private final HBox timePane = new HBox();
    private final Label labelForStart = new Label();
	private final Label labelForEnd = new Label();
	final Button editButton;
	final Button continueButton;
	final Button deleteButton;
    final Button stopButton;
    private final Node startToFinishActivityGraphics;
    private final Node ongoingActivityGraphics;
    private final Predicate<TimeTrackingItem> lastItemOfDay;
    private final BorderPane lastItemOnDayPane;

    private final Node newDayNode;

    TimeTrackingItemCell(Font fontAwesome,
                         ResourceBundle localization,
                         Predicate<TimeTrackingItem> lastItemOfDay,
                         ActionsHandler actionsHandler) {
        requireNonNull(fontAwesome);
        requireNonNull(actionsHandler);
        editButton = new FramelessButton(glyph(fontAwesome, Glyph.PENCIL, GLYPH_SIZE));
        continueButton = new FramelessButton(glyph(fontAwesome, Glyph.PLAY_CIRCLE, GLYPH_SIZE, Color.DARKGREEN));
        deleteButton = new FramelessButton(glyph(fontAwesome, Glyph.TRASH, GLYPH_SIZE, Color.web("e26868")));
        stopButton = new FramelessButton(glyph(fontAwesome, Glyph.STOP_CIRCLE, GLYPH_SIZE, Color.GOLDENROD));
        this.lastItemOfDay = requireNonNull(lastItemOfDay);
        this.startToFinishActivityGraphics = glyph(fontAwesome, Glyph.FAST_FORWARD, GLYPH_SIZE);
        this.ongoingActivityGraphics = glyph(fontAwesome, Glyph.FORWARD, GLYPH_SIZE);
        setupTooltips(localization);

        continueButton.setOnAction(event -> actionsHandler.continueItem(getItem()));
        editButton.setOnAction(event -> actionsHandler.edit(getItem()));
        deleteButton.setOnAction(event -> actionsHandler.delete(getItem()));
        stopButton.setOnAction(event -> actionsHandler.stop(getItem()));

		Pane space = new Pane();
		HBox.setHgrow(space, Priority.ALWAYS);
		labelForComment.setWrapText(true);
		labelForComment.setPrefWidth(350);

        timePane.setPrefWidth(180);
        timePane.setSpacing(10);
        timePane.setAlignment(Pos.CENTER_LEFT);

        cellPane.getChildren().addAll(
                continueButton,
                editButton,
                labelForComment,
                space,
                timePane,
                deleteButton);
        cellPane.setAlignment(Pos.CENTER_LEFT);

        lastItemOnDayPane = new BorderPane();

        HBox newDayHbox = new HBox(5);
        newDayHbox.getChildren().add(glyph(fontAwesome, Glyph.CALENDAR));
        Label dayLabel = new Label();
        dayLabel.textProperty().bind(Bindings.createStringBinding(
                () -> itemProperty().get() == null ? "" : DATE_FORMATTER.format(itemProperty().get().getStart()),
                itemProperty()));
        newDayHbox.getChildren().add(dayLabel);
        newDayNode = newDayHbox;

		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
	}

    protected void setupTooltips(ResourceBundle localization) {
        editButton.setTooltip(new Tooltip(localization
                .getString("itemList.edit")));
        continueButton.setTooltip(new Tooltip(localization
				.getString("itemList.continue")));
		deleteButton.setTooltip(new Tooltip(localization
				.getString("itemList.delete")));
        stopButton.setTooltip(new Tooltip(localization
                .getString("itemList.stop")));
    }

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
            cellPane.getChildren().set(0, item.getEnd().isPresent() ? continueButton : stopButton);
            applyLabelForComment();
            setupTimePane();
            if (lastItemOfDay.test(item)) {
                setupLastItemOfDayPane();
                setGraphic(lastItemOnDayPane);
            } else {
                setupCellPane();
                setGraphic(cellPane);
			}
		}
	}

	private void setupCellPane() {
        lastItemOnDayPane.setCenter(null);
        lastItemOnDayPane.setTop(null);
    }

    private void setupLastItemOfDayPane() {
        lastItemOnDayPane.setCenter(cellPane);
        lastItemOnDayPane.setTop(newDayNode);
    }

	private void applyLabelForComment() {
        labelForComment.setText(getItem().getActivity());
    }

	private void setupTimePane() {
        labelForStart.setText(TIME_FORMATTER.format(getItem().getStart()));

        if (getItem().getEnd().isPresent()) {
            labelForEnd.setText(TIME_FORMATTER.format(getItem().getEnd()
                    .get()));
            timePane.getChildren().setAll(labelForStart, startToFinishActivityGraphics,
                    labelForEnd);
        } else {
            timePane.getChildren().setAll(labelForStart, ongoingActivityGraphics);
        }
    }

    public interface ActionsHandler {
        void continueItem(TimeTrackingItem item);

        void edit(TimeTrackingItem item);

        void delete(TimeTrackingItem item);

        void stop(TimeTrackingItem item);
    }
}
