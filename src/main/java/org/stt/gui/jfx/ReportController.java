package org.stt.gui.jfx;

import javafx.animation.PauseTransition;
import javafx.beans.binding.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.controlsfx.control.NotificationPane;
import org.stt.config.ReportConfig;
import org.stt.gui.jfx.binding.MappedListBinding;
import org.stt.gui.jfx.binding.ReportBinding;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.text.ItemGrouper;
import org.stt.time.DateTimes;
import org.stt.time.DurationRounder;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.stt.time.DateTimes.FORMATTER_PERIOD_HHh_MMm_SSs;

public class ReportController {
    private final TimeTrackingItemQueries timeTrackingItemQueries;

    private final DurationRounder rounder;
    private final ItemGrouper itemGrouper;
    private final Color[] groupColors;
    private final ResourceBundle localization;
    private ReportConfig config;

    @FXML
    private TableColumn<ListItem, String> columnForRoundedDuration;
    @FXML
    private TableColumn<ListItem, String> columnForDuration;
    @FXML
    private TableColumn<ListItem, String> columnForComment;
    @FXML
    private TableView<ListItem> tableForReport;
    @FXML
    private BorderPane borderPane;
    @FXML
    private Label startOfReport;
    @FXML
    private Label endOfReport;
    @FXML
    private Label uncoveredTime;
    @FXML
    private Label roundedDurationSum;
    @FXML
    private DatePicker datePicker;

    private NotificationPane panel;
    private Font fontaweSome;
    private PauseTransition notificationPause = new PauseTransition(javafx.util.Duration.seconds(2));
    ;

    @Inject
    ReportController(ResourceBundle localization,
                     TimeTrackingItemQueries searcher,
                     DurationRounder rounder,
                     ItemGrouper itemGrouper,
                     ReportConfig config,
                     @Named("glyph") Font fontaweSome) {
        this.localization = requireNonNull(localization);
        this.config = requireNonNull(config);
        this.timeTrackingItemQueries = requireNonNull(searcher);
        this.rounder = requireNonNull(rounder);
        this.itemGrouper = requireNonNull(itemGrouper);
        this.fontaweSome = requireNonNull(fontaweSome);

        List<String> colorStrings = config.getGroupColors();
        groupColors = new Color[colorStrings.size()];
        for (int i = 0; i < colorStrings.size(); i++) {
            groupColors[i] = Color.web(colorStrings.get(i));
        }

        loadAndInjectFXML();
    }

    private void loadAndInjectFXML() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/stt/gui/jfx/ReportPanel.fxml"), localization);
        loader.setController(this);
        Pane reportPane;
        try {
            reportPane = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        panel = new NotificationPane(reportPane);
        notificationPause.setOnFinished(event -> panel.hide());
    }

    public Node getPanel() {
        return panel;
    }

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());

        final ObservableValue<Report> reportModel = createReportModel();
        final StringBinding startBinding = createBindingForStartOfReport(reportModel);
        final StringBinding endBinding = createBindingForEndOfReport(reportModel);
        final ObjectBinding<Duration> uncoveredTimeBinding = createBindingForUncoveredTimeOfReport(reportModel);
        ObservableStringValue formattedUncoveredTimeBinding = STTBindings
                .formattedDuration(uncoveredTimeBinding);
        ObjectBinding<Color> uncoveredTimeTextFillBinding = new When(
                uncoveredTimeBinding.isEqualTo(Duration.ZERO)).then(
                Color.BLACK).otherwise(Color.RED);

        startOfReport.textProperty().bind(startBinding);
        endOfReport.textProperty().bind(endBinding);
        uncoveredTime.textFillProperty().bind(uncoveredTimeTextFillBinding);
        uncoveredTime.textProperty().bind(formattedUncoveredTimeBinding);
        startOfReport.setOnMouseClicked(event -> setClipboard(startBinding.get(), event.getScreenX(), event.getScreenY()));
        endOfReport.setOnMouseClicked(event -> setClipboard(endBinding.get(), event.getScreenX(), event.getScreenY()));

        ListBinding<ListItem> reportListModel = createReportingItemsListModel(reportModel);
        tableForReport.setItems(reportListModel);
        tableForReport.getSelectionModel().setCellSelectionEnabled(true);

        roundedDurationSum
                .textProperty()
                .bind(STTBindings
                        .formattedDuration(createBindingForRoundedDurationSum(reportListModel)));

        setRoundedDurationColumnCellFactoryToConvertDurationToString();
        setDurationColumnCellFactoryToConvertDurationToString();
        setCommentColumnCellFactory();

        presetSortingToAscendingCommentColumn();

        columnForComment.prefWidthProperty().bind(
                tableForReport.widthProperty().subtract(
                        columnForRoundedDuration.widthProperty().add(
                                columnForDuration.widthProperty())));
    }

    private ObservableValue<Report> createReportModel() {
        ObservableValue<LocalDate> nextDay = Bindings.createObjectBinding(
                () -> datePicker.getValue() != null ? datePicker
                        .getValue().plusDays(1) : null, datePicker.valueProperty());
        return new ReportBinding(datePicker.valueProperty(), nextDay, timeTrackingItemQueries);
    }

    private ListBinding<ListItem> createReportingItemsListModel(
            final ObservableValue<Report> report) {
        return new MappedListBinding<>(() -> report.getValue()
                .getReportingItems().stream()
                .map(reportingItem -> new ListItem(
                        reportingItem.getComment(), reportingItem.getDuration(),
                        rounder.roundDuration(reportingItem.getDuration())))
                .collect(Collectors.toList()), report);
    }


    private ObservableValue<Duration> createBindingForRoundedDurationSum(
            final ObservableList<ListItem> items) {
        return Bindings.createObjectBinding(() ->
                items.stream()
                        .map(ListItem::getRoundedDuration)
                        .reduce(Duration.ZERO, Duration::plus), items);
    }

    private ObjectBinding<Duration> createBindingForUncoveredTimeOfReport(
            final ObservableValue<Report> reportModel) {
        return Bindings.createObjectBinding(() ->
                reportModel.getValue().getUncoveredDuration(), reportModel);
    }

    private StringBinding createBindingForEndOfReport(
            final ObservableValue<Report> reportModel) {
        return Bindings.createStringBinding(() -> {
            LocalDateTime end = reportModel.getValue().getEnd();
            return end != null ? DateTimes.DATE_TIME_FORMATTER_HH_MM_SS
                    .format(end) : "";
        }, reportModel);
    }

    private StringBinding createBindingForStartOfReport(
            final ObservableValue<Report> reportModel) {
        return Bindings.createStringBinding(() -> {
            LocalDateTime start = reportModel.getValue().getStart();
            return start != null ? DateTimes.DATE_TIME_FORMATTER_HH_MM_SS
                    .format(start) : "";
        }, reportModel);
    }

    private void presetSortingToAscendingCommentColumn() {
        columnForComment.setSortType(SortType.ASCENDING);
        tableForReport.getSortOrder().add(columnForComment);
    }

    private void setCommentColumnCellFactory() {
        columnForComment
                .setCellValueFactory(new PropertyValueFactory<>(
                        "comment"));
        if (config.isGroupItems()) {
            setItemGroupingCellFactory();
        } else {
            addClickToCopy(columnForComment, (item, event) -> setClipboard(item.getComment(), event.getScreenX(), event.getScreenY()));
        }
    }

    private void addClickToCopy(TableColumn<ListItem, String> column, BiConsumer<ListItem, MouseEvent> clickHandler) {
        column.setCellFactory(param -> {
            TableCell<ListItem, String> tableCell = (TableCell<ListItem, String>) TableColumn.DEFAULT_CELL_FACTORY.call(param);
            tableCell.setOnMouseClicked(event -> {
                ListItem item = (ListItem) tableCell.getTableRow().getItem();
                if (item != null) {
                    clickHandler.accept(item, event);
                }
            });
            return tableCell;
        });
    }

    private void setItemGroupingCellFactory() {
        columnForComment.setCellFactory(param -> new CommentTableCell());
    }

    private void setClipboard(String comment, double screenX, double screenY) {
        ClipboardContent content = new ClipboardContent();
        content.putString(comment);
        setClipboardContentTo(content);

        notifyUserOfClipboardContent(comment);
    }

    private void notifyUserOfClipboardContent(String comment) {
        panel.setGraphic(Glyph.glyph(fontaweSome, Glyph.CLIPBOARD, 20));
        panel.setText(String.format("'%s'", comment));
        panel.show();
        notificationPause.playFromStart();
    }

    private void copyDurationToClipboard(Duration duration, double screenX, double screenY) {
        setClipboard(DateTimes.prettyPrintDuration(duration), screenX, screenY);
    }

    private void setClipboardContentTo(ClipboardContent content) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        clipboard.setContent(content);
    }

    private void setDurationColumnCellFactoryToConvertDurationToString() {
        columnForDuration
                .setCellValueFactory(new PropertyValueFactory<ListItem, String>(
                        "duration") {
                    @Override
                    public ObservableValue<String> call(
                            CellDataFeatures<ListItem, String> cellDataFeatures) {
                        String duration = FORMATTER_PERIOD_HHh_MMm_SSs
                                .print(cellDataFeatures.getValue()
                                        .getDuration());
                        return new SimpleStringProperty(duration);
                    }
                });
        addClickToCopy(columnForDuration, (item, event) -> copyDurationToClipboard(item.getDuration(), event.getScreenX(), event.getScreenY()));
    }

    private void setRoundedDurationColumnCellFactoryToConvertDurationToString() {
        columnForRoundedDuration
                .setCellValueFactory(new PropertyValueFactory<ListItem, String>(
                        "roundedDuration") {
                    @Override
                    public ObservableValue<String> call(
                            CellDataFeatures<ListItem, String> cellDataFeatures) {
                        String duration = FORMATTER_PERIOD_HHh_MMm_SSs
                                .print(cellDataFeatures.getValue()
                                        .getRoundedDuration());
                        return new SimpleStringProperty(duration);
                    }
                });
        addClickToCopy(columnForRoundedDuration, (item, event) -> copyDurationToClipboard(item.getRoundedDuration(), event.getScreenX(), event.getScreenY()));
    }

    private class CommentTableCell extends TableCell<ListItem, String> {
        private TextFlow textFlow = new TextFlow() {
            // Textflow bug: Tries to place each character on a separate line if != USE_COMPUTED_SIZE by delivering width < -1...
            @Override
            protected double computePrefHeight(double width) {
                if (width > USE_COMPUTED_SIZE) {
                    return super.computePrefHeight(width);
                }
                CommentTableCell parent = (CommentTableCell) getParent();
                double prefWidth = parent.computePrefWidth(USE_COMPUTED_SIZE);
                Insets insets = parent.getInsets();
                // See javafx.scene.control.Control.layoutChildren()
                prefWidth = snapSize(prefWidth) - snapSize(insets.getLeft()) - snapSize(insets.getRight());
                return super.computePrefHeight(prefWidth);
            }
        };

        CommentTableCell() {
            setGraphic(textFlow);

            setOnMouseClicked(event -> {
                ListItem item = (ListItem) CommentTableCell.this.getTableRow().getItem();
                if (item == null) {
                    return;
                }
                setClipboard(item.getComment(), event.getScreenX(), event.getScreenY());
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                ObservableList<Node> textList = textFlow.getChildren();
                textList.clear();
                final List<String> itemGroups = itemGrouper.getGroupsOf(item);
                for (int i = 0; i < itemGroups.size(); i++) {
                    String partToShow;
                    String part = itemGroups.get(i);
                    if (i > 0) {
                        partToShow = " " + part;
                    } else {
                        partToShow = part;
                    }
                    final Text partLabel = new Text(partToShow);
                    addClickListener(itemGroups, partLabel, i);
                    if (i < groupColors.length) {
                        Color color = groupColors[i];
                        Color selected = color.deriveColor(0, 1, 3, 1);
                        BooleanBinding selectedRow = Bindings.equal(tableForReport.getSelectionModel().selectedIndexProperty(), indexProperty());
                        ObjectBinding<Color> colorObjectBinding = new When(selectedRow).then(selected).otherwise(color);
                        partLabel.fillProperty().bind(colorObjectBinding);
                    }
                    textList.add(partLabel);
                }
                setGraphic(textFlow);
            }
        }

        private void addClickListener(final List<String> itemGroups, Node partLabel, final int fromIndex) {
            partLabel.setOnMouseClicked(event -> {
                String commentRemainder = String.join(" ", itemGroups.subList(fromIndex, itemGroups.size()));
                setClipboard(commentRemainder, event.getScreenX(), event.getScreenY());
                event.consume();
            });
        }
    }

    public static class ListItem {

        private final String comment;
        private final Duration duration;
        private final Duration roundedDuration;

        public ListItem(String comment, Duration duration,
                        Duration roundedDuration) {
            this.comment = comment;
            this.duration = duration;
            this.roundedDuration = roundedDuration;
        }

        public String getComment() {
            return comment;
        }

        public Duration getDuration() {
            return duration;
        }

        public Duration getRoundedDuration() {
            return roundedDuration;
        }
    }
}
