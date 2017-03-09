package org.stt.gui.jfx;

import javafx.beans.binding.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.stt.config.ReportWindowConfig;
import org.stt.gui.jfx.binding.MappedListBinding;
import org.stt.gui.jfx.binding.ReportBinding;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.text.ItemGrouper;
import org.stt.time.DateTimes;
import org.stt.time.DurationRounder;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static org.stt.time.DateTimes.FORMATTER_PERIOD_HHh_MMm_SSs;

public class ReportWindowBuilder {
    private final TimeTrackingItemQueries timeTrackingItemQueries;

    private final Provider<Stage> stageProvider;
    private final DurationRounder rounder;
    private final ItemGrouper itemGrouper;
    private final Color[] groupColors;
    private ReportWindowConfig config;


    @Inject
    ReportWindowBuilder(Provider<Stage> stageProvider,
                        TimeTrackingItemQueries searcher,
                        DurationRounder rounder,
                        ItemGrouper itemGrouper,
                        ReportWindowConfig config) {
        this.config = Objects.requireNonNull(config);
        this.stageProvider = Objects.requireNonNull(stageProvider);
        this.timeTrackingItemQueries = Objects.requireNonNull(searcher);
        this.rounder = Objects.requireNonNull(rounder);
        this.itemGrouper = Objects.requireNonNull(itemGrouper);

        List<String> colorStrings = config.getGroupColors();
        groupColors = new Color[colorStrings.size()];
        for (int i = 0; i < colorStrings.size(); i++) {
            groupColors[i] = Color.web(colorStrings.get(i));
        }
    }

    public void setupStage() {
        Stage stage = stageProvider.get();

        ReportWindowController controller = new ReportWindowController(stage);

        ResourceBundle localization = ResourceBundle
                .getBundle("org.stt.gui.Application");
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/stt/gui/jfx/ReportPanel.fxml"), localization);
        loader.setController(controller);
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        stage.show();
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

    public class ReportWindowController {

        private final Stage stage;
        @FXML
        private TableColumn<ListItem, String> columnForRoundedDuration;
        @FXML
        private TableColumn<ListItem, String> columnForDuration;
        @FXML
        private TableColumn<ListItem, String> columnForComment;
        @FXML
        private TableView<ListItem> tableForReport;
        @FXML
        private FlowPane reportControlsPane;
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

        public ReportWindowController(Stage stage) {
            this.stage = Objects.requireNonNull(stage);
        }

        @FXML
        public void closeWindow() {
            stage.close();
        }

        @FXML
        public void initialize() {
            final ObservableValue<LocalDate> selectedDateTimeProperty = addComboBoxForDateTimeSelectionAndReturnSelectedDateTimeProperty();
            final ObservableValue<Report> reportModel = createReportModel(selectedDateTimeProperty);
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
            startOfReport.setOnMouseClicked(event -> setClipboard(startBinding.get()));
            endOfReport.setOnMouseClicked(event -> setClipboard(endBinding.get()));

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

            addSelectionToClipboardListenerToTableForReport();

            addSceneToStageAndSetStageToModal();

            columnForComment.prefWidthProperty().bind(
                    tableForReport.widthProperty().subtract(
                            columnForRoundedDuration.widthProperty().add(
                                    columnForDuration.widthProperty())));
        }

        private ObservableValue<Report> createReportModel(
                final ObservableValue<LocalDate> selectedDateTime) {
            ObservableValue<LocalDate> nextDay = Bindings.createObjectBinding(
                    () -> selectedDateTime.getValue() != null ? selectedDateTime
                            .getValue().plusDays(1) : null, selectedDateTime);
            return new ReportBinding(selectedDateTime, nextDay, timeTrackingItemQueries);
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
            return Bindings.createStringBinding(() ->
                    DateTimes.DATE_TIME_FORMATTER_HH_MM_SS
                            .format(reportModel.getValue().getEnd()), reportModel);
        }

        private StringBinding createBindingForStartOfReport(
                final ObservableValue<Report> reportModel) {
            return Bindings.createStringBinding(() ->
                    DateTimes.DATE_TIME_FORMATTER_HH_MM_SS
                            .format(reportModel.getValue().getStart()), reportModel);
        }

        private void addSceneToStageAndSetStageToModal() {
            Scene scene = new Scene(borderPane);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setScene(scene);

            scene.setOnKeyPressed(event -> {
                if (KeyCode.ESCAPE.equals(event.getCode())) {
                    event.consume();
                    stage.close();
                }
            });
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
            }
        }

        private void setItemGroupingCellFactory() {
            columnForComment.setCellFactory(param -> new CommentTableCell());
        }

        @SuppressWarnings("rawtypes")
        private void addSelectionToClipboardListenerToTableForReport() {
            tableForReport.getSelectionModel().getSelectedCells()
                    .addListener(new ListChangeListener<TablePosition>() {

                        @Override
                        public void onChanged(
                                javafx.collections.ListChangeListener.Change<? extends TablePosition> change) {
                            ObservableList<? extends TablePosition> selectedPositions = change
                                    .getList();
                            setClipboardIfExactlyOneItemWasSelected(selectedPositions);
                        }

                        private void setClipboardIfExactlyOneItemWasSelected(
                                ObservableList<? extends TablePosition> selectedPositions) {
                            if (selectedPositions.size() == 1) {
                                TablePosition position = selectedPositions
                                        .get(0);
                                ListItem listItem = tableForReport.getItems()
                                        .get(position.getRow());
                                if (position.getTableColumn() == columnForRoundedDuration) {
                                    copyDurationToClipboard(listItem.getRoundedDuration());
                                } else if (position.getTableColumn() == columnForDuration) {
                                    copyDurationToClipboard(listItem.getDuration());
                                } else if (position.getTableColumn() == columnForComment) {
                                    setClipboard(listItem.getComment());
                                }
                            }
                        }

                    });
        }

        private void setClipboard(String comment) {
            ClipboardContent content = new ClipboardContent();
            content.putString(comment);
            setClipboardContentTo(content);
        }

        private void copyDurationToClipboard(Duration duration) {
            setClipboard(DateTimes.prettyPrintDuration(duration));
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
        }

        private ObservableValue<LocalDate> addComboBoxForDateTimeSelectionAndReturnSelectedDateTimeProperty() {
            final ComboBox<LocalDate> comboBox = new ComboBox<>();
            ObservableList<LocalDate> availableDays = FXCollections
                    .observableArrayList(timeTrackingItemQueries.queryAllTrackedDays().collect(Collectors.toList()));
            Collections.reverse(availableDays);
            comboBox.setItems(availableDays);
            if (!availableDays.isEmpty()) {
                comboBox.getSelectionModel().select(0);
            }
            comboBox.setConverter(new StringConverter<LocalDate>() {
                @Override
                public String toString(LocalDate dateTime) {
                    return DateTimeFormatter.ISO_LOCAL_DATE.format(dateTime);
                }

                @Override
                public LocalDate fromString(String arg0) {
                    throw new UnsupportedOperationException();
                }
            });
            reportControlsPane.getChildren().add(comboBox);

            return comboBox.getSelectionModel().selectedItemProperty();
        }

        private class CommentTableCell extends TableCell<ListItem, String> {
            private FlowPane flowPane = new FlowPane();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                ObservableList<Node> flowPaneChildren = flowPane.getChildren();
                flowPaneChildren.clear();
                if (!empty) {
                    final List<String> itemGroups = itemGrouper.getGroupsOf(item);
                    for (int i = 0; i < itemGroups.size(); i++) {
                        String partToShow;
                        String part = itemGroups.get(i);
                        if (i > 0) {
                            partToShow = " " + part;
                        } else {
                            partToShow = part;
                        }
                        final Label partLabel = new Label(partToShow);
                        addClickListener(itemGroups, partLabel, i);
                        if (i < groupColors.length) {
                            Color color = groupColors[i];
                            Color selected = color.deriveColor(0, 1, 3, 1);
                            BooleanBinding selectedRow = Bindings.equal(tableForReport.getSelectionModel().selectedIndexProperty(), indexProperty());
                            ObjectBinding<Color> colorObjectBinding = new When(selectedRow).then(selected).otherwise(color);
                            partLabel.textFillProperty().bind(colorObjectBinding);
                        }
                        flowPaneChildren.add(partLabel);
                    }
                }
                setGraphic(flowPane);
            }

            private void addClickListener(final List<String> itemGroups, Label partLabel, final int fromIndex) {
                partLabel.setOnMouseClicked(event -> {
                    String commentRemainder = String.join(" ", itemGroups.subList(fromIndex, itemGroups.size()));
                    setClipboard(commentRemainder);
                });
            }
        }
    }
}
