package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.Configuration;
import org.stt.importer.DefaultItemImporter;
import org.stt.model.ReportingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemSearcher;
import org.stt.reporting.ReportGenerator;
import org.stt.reporting.SummingReportGenerator;
import org.stt.searching.DefaultItemSearcher;

import com.sun.javafx.application.PlatformImpl;

public class ReportWindow {
	private final Stage stage;
	private final ItemSearcher itemSearcher;
	private final ReportGenerator reportGenerator;

	private final PeriodFormatter hmsPeriodFormatter = new PeriodFormatterBuilder()
			.printZeroAlways().minimumPrintedDigits(2).appendHours()
			.appendSuffix("h").appendSeparator(":").appendMinutes()
			.appendSuffix("m").appendSeparator(":").appendSeconds()
			.appendSuffix("s").toFormatter();

	@FXML
	private TableColumn<ReportingItem, String> columnForDuration;

	@FXML
	private TableColumn<ReportingItem, String> columnForComment;

	@FXML
	private TableView<ReportingItem> tableForReport;

	public ReportWindow(Stage stage, ItemSearcher searcher,
			ReportGenerator reportGenerator) {
		this.itemSearcher = checkNotNull(searcher);
		this.reportGenerator = checkNotNull(reportGenerator);
		this.stage = checkNotNull(stage);
	}

	private void setupStage() throws IOException {
		ResourceBundle localization = ResourceBundle
				.getBundle("org.stt.gui.Application");
		FXMLLoader loader = new FXMLLoader(getClass().getResource(
				"/org/stt/gui/jfx/ReportWindow.fxml"), localization);
		loader.setController(this);
		BorderPane pane;
		pane = (BorderPane) loader.load();

		ComboBox<String> comboBox = new ComboBox<String>();
		DateTimeFormatter shortDate = DateTimeFormat.shortDate();
		for (DateTime day : itemSearcher.getAllTrackedDays()) {
			comboBox.getItems().add(0, shortDate.print(day));
		}
		pane.setTop(comboBox);

		List<ReportingItem> report = reportGenerator.report();
		tableForReport.getItems().setAll(report);
		columnForDuration
				.setCellValueFactory(new PropertyValueFactory<ReportingItem, String>(
						"duration") {
					@Override
					public ObservableValue<String> call(
							CellDataFeatures<ReportingItem, String> cellDataFeatures) {
						String duration = hmsPeriodFormatter
								.print(cellDataFeatures.getValue()
										.getDuration().toPeriod());
						return new SimpleStringProperty(duration);
					}
				});
		columnForComment
				.setCellValueFactory(new PropertyValueFactory<ReportingItem, String>(
						"comment"));
		tableForReport.getSelectionModel().getSelectedCells()
				.addListener(new ListChangeListener<TablePosition>() {

					@Override
					public void onChanged(
							javafx.collections.ListChangeListener.Change<? extends TablePosition> change) {
					}
				});

		Scene scene = new Scene(pane);
		stage.setScene(scene);
		stage.show();
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent arg0) {
				exitPlatform();
			}
		});
	}

	private static void exitPlatform() {
		Platform.exit();
		PlatformImpl.tkExit();
	}

	public static void main(String[] args) {
		new JFXPanel();
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				final Configuration configuration = new Configuration();
				Stage stage = new Stage();
				ItemReaderProvider itemReaderProvider = new ItemReaderProvider() {

					@Override
					public ItemReader provideReader() {
						try {
							return new DefaultItemImporter(new FileReader(
									configuration.getSttFile()));
						} catch (FileNotFoundException e) {
							throw new IllegalStateException(e);
						}
					}
				};
				DefaultItemSearcher searcher = new DefaultItemSearcher(
						itemReaderProvider);

				ItemReader readerForReport = itemReaderProvider.provideReader();
				ReportGenerator report = new SummingReportGenerator(
						readerForReport);
				ReportWindow window = new ReportWindow(stage, searcher, report);
				try {
					window.setupStage();
				} catch (IOException e) {
					exitPlatform();
				}
			}
		});
	}
}
