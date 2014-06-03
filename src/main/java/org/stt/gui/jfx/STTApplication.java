package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Calendar;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import org.stt.CommandHandler;
import org.stt.model.TimeTrackingItem;

import com.google.inject.Inject;

public class STTApplication {
	private Stage stage;
	@FXML
	TextArea commandText;
	@FXML
	ListView<TimeTrackingItem> history;

	private CommandHandler commandHandler;

	@Inject
	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@Inject
	public void setCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = checkNotNull(commandHandler);
	}

	public void setupStage() throws Exception {
		checkNotNull(stage);

		ResourceBundle localization = ResourceBundle
				.getBundle("org.stt.gui.Application");
		FXMLLoader loader = new FXMLLoader(getClass().getResource(
				"/org/stt/gui/jfx/MainWindow.fxml"), localization);
		loader.setController(this);

		BorderPane pane = (BorderPane) loader.load();

		history.setSelectionModel(new NoSelectionModel<TimeTrackingItem>());
		history.setCellFactory(new Callback<ListView<TimeTrackingItem>, ListCell<TimeTrackingItem>>() {

			@Override
			public ListCell<TimeTrackingItem> call(
					ListView<TimeTrackingItem> arg0) {
				return new TimeTrackingItemCell();
			}
		});
		ObservableList<TimeTrackingItem> items = history.getItems();
		items.addAll(new TimeTrackingItem("Test1", Calendar.getInstance()),
				new TimeTrackingItem("Test2", Calendar.getInstance()),
				new TimeTrackingItem("Test3", Calendar.getInstance()));

		Scene scene = new Scene(pane);
		stage.setScene(scene);
		stage.setTitle(localization.getString("window.title"));
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent arg0) {
				System.exit(0);
			}
		});
		stage.show();
	}

	@FXML
	protected void done() {
		stage.close();
		System.exit(0);
	}

	@FXML
	void onKeyPressed(KeyEvent event) {
		if (KeyCode.ENTER.equals(event.getCode()) && event.isControlDown()) {
			executeCommand();
		}
	}

	void executeCommand() {
		commandHandler.executeCommand(commandText.getText());
		commandText.clear();

	}

	public void start() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					setupStage();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
