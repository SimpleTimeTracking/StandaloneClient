package org.stt.gui.jfx;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.prism.NGNode;
import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.stt.model.TimeTrackingItem;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.stt.gui.jfx.Glyph.GLYPH_SIZE_MEDIUM;
import static org.stt.gui.jfx.Glyph.glyph;

class TimeTrackingItemCellWithActions extends ListCell<TimeTrackingItem> {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);

    private final HBox cellPane = new HBox(2);
    final Button editButton;
    final Button continueButton;
    final Button deleteButton;
    final Button stopButton;
    private final Predicate<TimeTrackingItem> lastItemOfDay;
    private final BorderPane lastItemOnDayPane;

    private final Node newDayNode;
    private final TimeTrackingItemNodes itemNodes;
    private final int buttonIndex;

    TimeTrackingItemCellWithActions(Font fontAwesome,
                                    ResourceBundle localization,
                                    Predicate<TimeTrackingItem> lastItemOfDay,
                                    ActionsHandler actionsHandler,
                                    ActivityTextDisplayProcessor labelToNodeMapper) {
        requireNonNull(fontAwesome);
        requireNonNull(actionsHandler);
        itemNodes = new TimeTrackingItemNodes(labelToNodeMapper, TIME_FORMATTER, fontAwesome, 450, 180, localization);
        editButton = new FramelessButton(glyph(fontAwesome, Glyph.PENCIL, GLYPH_SIZE_MEDIUM));
        continueButton = new FramelessButton(glyph(fontAwesome, Glyph.PLAY_CIRCLE, GLYPH_SIZE_MEDIUM, Color.DARKGREEN));
        deleteButton = new FramelessButton(glyph(fontAwesome, Glyph.TRASH, GLYPH_SIZE_MEDIUM, Color.web("e26868")));
        stopButton = new FramelessButton(glyph(fontAwesome, Glyph.STOP_CIRCLE, GLYPH_SIZE_MEDIUM, Color.GOLDENROD));
        this.lastItemOfDay = requireNonNull(lastItemOfDay);
        setupTooltips(localization);

        continueButton.setOnAction(event -> actionsHandler.continueItem(getItem()));
        editButton.setOnAction(event -> actionsHandler.edit(getItem()));
        deleteButton.setOnAction(event -> actionsHandler.delete(getItem()));
        stopButton.setOnAction(event -> actionsHandler.stop(getItem()));

        itemNodes.appendNodesTo(cellPane.getChildren());

        buttonIndex = cellPane.getChildren().size();

        cellPane.getChildren().addAll(
                continueButton,
                editButton,
                deleteButton);
        cellPane.setAlignment(Pos.CENTER_LEFT);

        DoubleProperty opacityProperty = fadeOnHoverProperty();

        Stream.of(continueButton, editButton, deleteButton, stopButton)
                .map(Node::opacityProperty)
                .forEach(p -> p.bind(opacityProperty));

        lastItemOnDayPane = new BorderPane();

        newDayNode = createDateSubheader(fontAwesome);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private DoubleProperty fadeOnHoverProperty() {
        Node placeHolder = new DummyNode();
        placeHolder.setOpacity(0);

        FadeTransition fade = new FadeTransition();
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setNode(placeHolder);
        cellPane.hoverProperty().addListener((observable, oldValue, newValue) -> {
            fade.setRate(newValue ? 1 : -1);
            fade.play();
        });
        return placeHolder.opacityProperty();
    }

    private Node createDateSubheader(Font fontAwesome) {
        HBox newDayHbox = new HBox(5);
        newDayHbox.setPadding(new Insets(2));
        Label calenderIcon = glyph(fontAwesome, Glyph.CALENDAR);
        calenderIcon.setTextFill(Color.BLACK);
        newDayHbox.getChildren().add(calenderIcon);
        Label dayLabel = new Label();
        dayLabel.setTextFill(Color.BLACK);
        dayLabel.textProperty().bind(Bindings.createStringBinding(
                () -> itemProperty().get() == null ? "" : DATE_FORMATTER.format(itemProperty().get().getStart()),
                itemProperty()));
        newDayHbox.getChildren().add(dayLabel);
        newDayHbox.setBackground(new Background(new BackgroundFill(Color.LIGHTSTEELBLUE, null, null)));
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
            cellPane.getChildren().set(buttonIndex, item.getEnd().isPresent() ? continueButton : stopButton);
            itemNodes.setItem(item);
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

    public interface ActionsHandler {
        void continueItem(TimeTrackingItem item);

        void edit(TimeTrackingItem item);

        void delete(TimeTrackingItem item);

        void stop(TimeTrackingItem item);
    }

    private static class DummyNode extends Node {
        @Override
        protected NGNode impl_createPeer() {
            return null;
        }

        @Override
        public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
            return null;
        }

        @Override
        protected boolean impl_computeContains(double localX, double localY) {
            return false;
        }

        @Override
        public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
            return null;
        }
    }
}
