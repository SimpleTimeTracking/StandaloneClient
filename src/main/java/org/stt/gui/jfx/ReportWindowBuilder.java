package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.Factory;
import org.stt.gui.jfx.binding.ReportBinding;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.model.ReportingItem;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.searching.ItemSearcher;
import org.stt.time.DateTimeHelper;
import static org.stt.time.DateTimeHelper.FORMATTER_PERIOD_HHh_MMm_SSs;
import org.stt.time.DurationRounder;

public class ReportWindowBuilder {

	private final ItemReaderProvider readerProvider;
	private final ItemSearcher itemSearcher;

	private final Factory<Stage> stageFactory;
	private final DurationRounder rounder;

	public ReportWindowBuilder(Factory<Stage> stageFactory,
			ItemReaderProvider readerProvider, ItemSearcher searcher, DurationRounder rounder) {
		this.stageFactory = checkNotNull(stageFactory);
		this.itemSearcher = checkNotNull(searcher);
		this.readerProvider = checkNotNull(readerProvider);
		this.rounder = checkNotNull(rounder);
	}

	public void setupStage() throws IOException {
		Stage stage = stageFactory.create();

		ReportWindowController controller = new ReportWindowController(stage);

		ResourceBundle localization = ResourceBundle
				.getBundle("org.stt.gui.Application");
		FXMLLoader loader = new FXMLLoader(getClass().getResource(
				"/org/stt/gui/jfx/ReportWindow.fxml"), localization);
		loader.setController(controller);
		loader.load();

		stage.show();
	}

	private ObservableValue<Report> createReportModel(
			final ObservableValue<DateTime> selectedDateTime) {
		ObservableValue<DateTime> nextDay = new ObjectBinding<DateTime>() {
			{
				bind(selectedDateTime);
			}

			@Override
			protected DateTime computeValue() {
				return selectedDateTime.getValue() != null ? selectedDateTime.getValue().plusDays(1) : null;
			}
		};
		return new ReportBinding(selectedDateTime, nextDay, readerProvider);
	}

	private ListBinding<ListItem> createReportingItemsListModel(
			final ObservableValue<Report> report) {
		return new ListBinding<ListItem>() {
			{
				super.bind(report);
			}

			@Override
			protected ObservableList<ListItem> computeValue() {
				List<ReportingItem> reportingItems = report.getValue().getReportingItems();
				List<ListItem> resultList = new ArrayList<>();
				for (ReportingItem reportItem : reportingItems) {
					resultList.add(new ListItem(reportItem.getComment(), reportItem.getDuration(), rounder.roundDuration(reportItem.getDuration())));
				}
				return FXCollections.observableArrayList(resultList);
			}

		};
	}

	public static class ListItem {

		private final String comment;
		private final Duration duration;
		private final Duration roundedDuration;

		public ListItem(String comment, Duration duration, Duration roundedDuration) {
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

		private final Stage stage;

		public ReportWindowController(Stage stage) {
			this.stage = checkNotNull(stage);
		}

		@FXML
		public void closeWindow() {
			stage.close();
		}

		@FXML
		public void initialize() {
			final ObservableValue<DateTime> selectedDateTimeProperty = addComboBoxForDateTimeSelectionAndReturnSelectedDateTimeProperty();
			final ObservableValue<Report> reportModel = createReportModel(selectedDateTimeProperty);
			StringBinding startBinding = createBindingForStartOfReport(reportModel);
			StringBinding endBinding = createBindingForEndOfReport(reportModel);
			final ObjectBinding<Duration> uncoveredTimeBinding
					= createBindingForUncoveredTimeOfReport(reportModel);
			ObservableStringValue formattedUncoveredTimeBinding = STTBindings.formattedDuration(uncoveredTimeBinding);
			ObjectBinding<Color> uncoveredTimeTextFillBinding = new When(
					uncoveredTimeBinding.isEqualTo(Duration.ZERO)).then(
							Color.BLACK).otherwise(Color.RED);

			startOfReport.textProperty().bind(startBinding);
			endOfReport.textProperty().bind(endBinding);
			uncoveredTime.textFillProperty().bind(uncoveredTimeTextFillBinding);
			uncoveredTime.textProperty().bind(formattedUncoveredTimeBinding);

			ListBinding<ListItem> reportListModel = createReportingItemsListModel(reportModel);
			tableForReport.setItems(reportListModel);

			roundedDurationSum.textProperty().bind(STTBindings.formattedDuration(createBindingForRoundedDurationSum(reportListModel)));

			setRoundedDurationColumnCellFactoryToConvertDurationToString();
			setDurationColumnCellFactoryToConvertDurationToString();
			setCommentColumnCellFactory();

			presetSortingToAscendingCommentColumn();

			addSelectionToClipboardListenerToTableForReport();

			addSceneToStageAndSetStageToModal();

			columnForComment.prefWidthProperty()
					.bind(tableForReport.widthProperty().subtract(columnForRoundedDuration.widthProperty().add(columnForDuration.widthProperty())));
		}

		private ObservableValue<Duration> createBindingForRoundedDurationSum(final ListBinding<ListItem> items) {
			return new ObjectBinding<Duration>() {
				{
					bind(items);
				}

				@Override
				protected Duration computeValue() {
					Duration duration = Duration.ZERO;
					for (ListItem item : items) {
						duration = duration.plus(item.roundedDuration);
					}
					return duration;
				}
			};
		}

		private ObjectBinding<Duration> createBindingForUncoveredTimeOfReport(
				final ObservableValue<Report> reportModel) {
			return new ObjectBinding<Duration>() {
				{
					bind(reportModel);
				}

				@Override
				protected Duration computeValue() {
					return reportModel.getValue().getUncoveredDuration();
				}
			};
		}

		private StringBinding createBindingForEndOfReport(
				final ObservableValue<Report> reportModel) {
			StringBinding endBinding = new StringBinding() {
				{
					bind(reportModel);
				}

				@Override
				protected String computeValue() {
					return DateTimeHelper.DATE_TIME_FORMATTER_HH_MM_SS.print(reportModel.getValue().getEnd());
				}
			};
			return endBinding;
		}

		private StringBinding createBindingForStartOfReport(
				final ObservableValue<Report> reportModel) {
			StringBinding startBinding = new StringBinding() {
				{
					bind(reportModel);
				}

				@Override
				protected String computeValue() {
					return DateTimeHelper.DATE_TIME_FORMATTER_HH_MM_SS.print(reportModel.getValue()
							.getStart());
				}
			};
			return startBinding;
		}

		private void addSceneToStageAndSetStageToModal() {
			Scene scene = new Scene(borderPane);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initStyle(StageStyle.UTILITY);
			stage.setScene(scene);
		}

		private void presetSortingToAscendingCommentColumn() {
			columnForComment.setSortType(SortType.ASCENDING);
			tableForReport.getSortOrder().add(columnForComment);
		}

		private void setCommentColumnCellFactory() {
			columnForComment
					.setCellValueFactory(new PropertyValueFactory<ListItem, String>(
									"comment"));
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
												ListItem listItem = tableForReport
												.getItems().get(position.getRow());
												if (position.getTableColumn() == columnForRoundedDuration) {
													setClipBoard(listItem.getRoundedDuration());
												} else if (position.getTableColumn() == columnForDuration) {
													setClipBoard(listItem.getDuration());
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

		private void setClipBoard(Duration duration) {
			ClipboardContent content = new ClipboardContent();
			PeriodFormatter formatter = new PeriodFormatterBuilder()
					.printZeroIfSupported().minimumPrintedDigits(2).appendHours().appendSeparator(":")
					.appendMinutes().toFormatter();
			content.putString(formatter.print(duration.toPeriod()));
			setClipboardContentTo(content);
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
															.getDuration().toPeriod());
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
															.getRoundedDuration().toPeriod());
													return new SimpleStringProperty(duration);
												}
									});
		}

		private ObservableValue<DateTime> addComboBoxForDateTimeSelectionAndReturnSelectedDateTimeProperty() {
			final ComboBox<DateTime> comboBox = new ComboBox<>();
			ObservableList<DateTime> availableDays = FXCollections
					.observableArrayList(itemSearcher.getAllTrackedDays());
			Collections.reverse(availableDays);
			comboBox.setItems(availableDays);
			if (!availableDays.isEmpty()) {
				comboBox.getSelectionModel().select(0);
			}
			comboBox.setConverter(new StringConverter<DateTime>() {
				@Override
				public String toString(DateTime dateTime) {
					return DateTimeFormat.shortDate().print(dateTime);
				}

				@Override
				public DateTime fromString(String arg0) {
					throw new UnsupportedOperationException();
				}
			});
			reportControlsPane.getChildren().add(comboBox);

			return comboBox.getSelectionModel().selectedItemProperty();
		}
	}
}
