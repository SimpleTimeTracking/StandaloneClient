package org.stt.gui.jfx;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.stt.gui.UIMain;
import org.stt.model.TimeTrackingItem;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.stt.gui.jfx.Glyph.GLYPH_SIZE_MEDIUM;
import static org.stt.gui.jfx.Glyph.glyph;

class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final HBox cellPane = new HBox(2);
    private final TextFlow labelForComment = new TextFlow();
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
    private final Function<Object, Stream<Object>> labelToNodeMapper;

    TimeTrackingItemCell(Font fontAwesome,
                         ResourceBundle localization,
                         Predicate<TimeTrackingItem> lastItemOfDay,
                         ActionsHandler actionsHandler,
                         ActivityTextDisplayProcessor labelToNodeMapper) {
        requireNonNull(fontAwesome);
        requireNonNull(actionsHandler);
        this.labelToNodeMapper = requireNonNull(labelToNodeMapper);
        editButton = new FramelessButton(glyph(fontAwesome, Glyph.PENCIL, GLYPH_SIZE_MEDIUM));
        continueButton = new FramelessButton(glyph(fontAwesome, Glyph.PLAY_CIRCLE, GLYPH_SIZE_MEDIUM, Color.DARKGREEN));
        deleteButton = new FramelessButton(glyph(fontAwesome, Glyph.TRASH, GLYPH_SIZE_MEDIUM, Color.web("e26868")));
        stopButton = new FramelessButton(glyph(fontAwesome, Glyph.STOP_CIRCLE, GLYPH_SIZE_MEDIUM, Color.GOLDENROD));
        this.lastItemOfDay = requireNonNull(lastItemOfDay);
        this.startToFinishActivityGraphics = glyph(fontAwesome, Glyph.FAST_FORWARD, GLYPH_SIZE_MEDIUM);
        this.ongoingActivityGraphics = glyph(fontAwesome, Glyph.FORWARD, GLYPH_SIZE_MEDIUM);
        setupTooltips(localization);

        continueButton.setOnAction(event -> actionsHandler.continueItem(getItem()));
        editButton.setOnAction(event -> actionsHandler.edit(getItem()));
        deleteButton.setOnAction(event -> actionsHandler.delete(getItem()));
        stopButton.setOnAction(event -> actionsHandler.stop(getItem()));

        Pane space = new Pane();
        HBox.setHgrow(space, Priority.ALWAYS);
        timePane.setPrefWidth(180);
        timePane.setSpacing(10);
        timePane.setAlignment(Pos.CENTER_LEFT);

        StackPane labelArea = new StackPaneWithoutResize(labelForComment);
        StackPane.setAlignment(labelForComment, Pos.CENTER_LEFT);

        labelArea.setPrefWidth(450);

        cellPane.getChildren().addAll(
                labelArea,
                space,
                timePane,
                continueButton,
                editButton,
                deleteButton);
        cellPane.setAlignment(Pos.CENTER_LEFT);
        if (UIMain.DEBUG_UI) {
            labelArea.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderStroke.MEDIUM)));
            labelForComment.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderStroke.MEDIUM)));
        }

        lastItemOnDayPane = new BorderPane();

        newDayNode = createDateSubheader(fontAwesome);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private Node createDateSubheader(Font fontAwesome) {
        HBox newDayHbox = new HBox(5);
        newDayHbox.setPadding(new Insets(2));
        Label calenderIcon = glyph(fontAwesome, Glyph.CALENDAR);
        calenderIcon.setTextFill(Color.CORNFLOWERBLUE);
        newDayHbox.getChildren().add(calenderIcon);
        Label dayLabel = new Label();
        dayLabel.setTextFill(Color.CORNFLOWERBLUE);
        dayLabel.textProperty().bind(Bindings.createStringBinding(
                () -> itemProperty().get() == null ? "" : DATE_FORMATTER.format(itemProperty().get().getStart()),
                itemProperty()));
        newDayHbox.getChildren().add(dayLabel);
        return newDayHbox;
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
            cellPane.getChildren().set(3, item.getEnd().isPresent() ? continueButton : stopButton);
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
        BorderPane.setMargin(newDayNode, new Insets(10, 0, 10, 0));
    }

    private void applyLabelForComment() {
        List<Node> textNodes = labelToNodeMapper.apply(getItem().getActivity())
                .map(o -> {
                    if (o instanceof String) {
                        return new Text((String) o);
                    }
                    if (o instanceof Node) {
                        return (Node) o;
                    }
                    throw new IllegalArgumentException(String.format("Unsupported element: %s", o));
                })
                .collect(Collectors.toList());
        labelForComment.getChildren().setAll(textNodes);
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
