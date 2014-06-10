package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
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

public class STTApplication {
	private final Stage stage;
	@FXML
	TextArea commandText;
	@FXML
	ListView<TimeTrackingItem> history;

	private final ExecutorService executorService;

	private final CommandHandler commandHandler;
	private final ItemReader historySource;

	public STTApplication(Stage stage, CommandHandler commandHandler,
			ItemReader historySource, ExecutorService executorService) {
		this.stage = checkNotNull(stage);
		this.commandHandler = checkNotNull(commandHandler);
		this.historySource = checkNotNull(historySource);
		this.executorService = checkNotNull(executorService);
	}

	public void readHistoryFrom(final ItemReader reader) {
		executorService.execute(new Task<Collection<TimeTrackingItem>>() {
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

		history.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		history.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<TimeTrackingItem>() {

					@Override
					public void changed(
							ObservableValue<? extends TimeTrackingItem> observable,
							TimeTrackingItem oldItem, TimeTrackingItem newItem) {
						if (newItem != null) {
							history.getSelectionModel().clearSelection();
							if (newItem.getComment().isPresent()) {
								commandText.setText(newItem.getComment().get());
								commandText.positionCaret(commandText
										.getLength());
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										commandText.requestFocus();
									}
								});
							}
						}

					}
				});
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
		Platform.exit();
		executorService.shutdown();
		try {
			executorService.awaitTermination(1, TimeUnit.SECONDS);
			commandHandler.close();
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
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
