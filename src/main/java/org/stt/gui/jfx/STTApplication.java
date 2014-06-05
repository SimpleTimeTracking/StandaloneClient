package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class STTApplication {
	private Stage stage;
	@FXML
	TextArea commandText;
	@FXML
	ListView<TimeTrackingItem> history;

	private ExecutorService service;

	private CommandHandler commandHandler;
	private ItemReader historySource;

	@Inject
	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@Inject
	public void setCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = checkNotNull(commandHandler);
	}

	@Inject(optional = true)
	public void setHistorySource(@Named("dataSource") ItemReader reader) {
		this.historySource = reader;
	}

	@Inject
	public void setExecutorService(ExecutorService service) {
		this.service = checkNotNull(service);
	}

	public void readHistoryFrom(final ItemReader reader) {
		service.execute(new Task<Collection<TimeTrackingItem>>() {
			@Override
			protected Collection<TimeTrackingItem> call() throws Exception {
				return IOUtil.readAll(reader);
			}

			@Override
			protected void succeeded() {
				ObservableList<TimeTrackingItem> items = history.getItems();
				items.clear();
				items.addAll(getValue());
			}

			@Override
			protected void failed() {
				getException().printStackTrace();
			}
		});
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
					readHistoryFrom(historySource);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

}
