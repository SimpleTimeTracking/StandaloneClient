package org.stt.gui.jfx;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.controlsfx.validation.decoration.GraphicValidationDecoration;
import org.stt.gui.UIMain;
import org.stt.model.TimeTrackingItem;
import org.stt.time.DateTimes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.stt.gui.jfx.Glyph.GLYPH_SIZE_MEDIUM;
import static org.stt.gui.jfx.Glyph.glyph;

/**
 * Created by dante on 01.07.17.
 */
public class TimeTrackingItemNodes {
    private static final Image WARNING_IMAGE = new Image(GraphicValidationDecoration.class.getResource("/impl/org/controlsfx/control/validation/decoration-warning.png").toExternalForm()); //$NON-NLS-1$

    private final Function<Stream<Object>, Stream<Object>> labelToNodeMapper;
    private final TextFlow labelForComment = new TextFlow();
    private final HBox timePane = new HBox();
    private final Label labelForStart = new Label();
    private final Label labelForEnd = new Label();
    private final Node startToFinishActivityGraphics;
    private final Control ongoingActivityGraphics;
    private final ResourceBundle localization;
    private final DateTimeFormatter dateTimeFormatter;
    private final Pane space;
    private final Pane labelArea;

    public TimeTrackingItemNodes(Function<Stream<Object>, Stream<Object>> labelToNodeMapper,
                                 DateTimeFormatter dateTimeFormatter,
                                 Font fontAwesome,
                                 int labelAreaWidth,
                                 int timePaneWidth,
                                 ResourceBundle localization) {
        this.labelToNodeMapper = requireNonNull(labelToNodeMapper);
        this.dateTimeFormatter = requireNonNull(dateTimeFormatter);
        this.localization = requireNonNull(localization);

        startToFinishActivityGraphics = glyph(fontAwesome, Glyph.FAST_FORWARD, GLYPH_SIZE_MEDIUM);
        ongoingActivityGraphics = glyph(fontAwesome, Glyph.FORWARD, GLYPH_SIZE_MEDIUM);

        labelArea = new StackPaneWithoutResize(labelForComment);
        StackPane.setAlignment(labelForComment, Pos.CENTER_LEFT);

        HBox.setHgrow(labelArea, Priority.ALWAYS);
        labelArea.setMaxWidth(labelAreaWidth);

        space = new Pane();
        HBox.setHgrow(space, Priority.SOMETIMES);
        timePane.setPrefWidth(timePaneWidth);
        timePane.setSpacing(10);
        timePane.setAlignment(Pos.CENTER_LEFT);

        if (UIMain.DEBUG_UI) {
            labelArea.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderStroke.MEDIUM)));
            labelForComment.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderStroke.MEDIUM)));
        }
    }

    private void applyLabelForComment(String activity) {
        List<Node> textNodes = labelToNodeMapper.apply(Stream.of(activity))
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


    public void appendNodesTo(ObservableList<Node> parent) {
        parent.addAll(labelArea, space, timePane);

    }

    public void setItem(TimeTrackingItem item) {
        applyLabelForComment(item.getActivity());
        setupTimePane(item);
    }

    private void setupTimePane(TimeTrackingItem item) {
        labelForStart.setText(dateTimeFormatter.format(item.getStart()));

        Optional<LocalDateTime> end = item.getEnd();
        if (end.isPresent()) {
            labelForEnd.setText(dateTimeFormatter.format(end.get()));
            timePane.getChildren().setAll(labelForStart, startToFinishActivityGraphics, labelForEnd);
        } else {

            if (!DateTimes.isOnSameDay(item.getStart(), LocalDateTime.now())) {
                StackPane stackPane = new StackPane(ongoingActivityGraphics);
                Node hallo = new ImageView(WARNING_IMAGE);
                Tooltips.install(stackPane, localization.getString("ongoingActivityDidntStartToday"));
                stackPane.getChildren().add(hallo);
                StackPane.setAlignment(hallo, Pos.TOP_RIGHT);
                timePane.getChildren().setAll(labelForStart, stackPane);
            } else {
                timePane.getChildren().setAll(labelForStart, ongoingActivityGraphics);
            }
        }
    }

}
