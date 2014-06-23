package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.binding.ListBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import org.stt.CommandHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.ContinueActionHandler;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;
import org.stt.searching.CommentSearcher;

import com.sun.javafx.application.PlatformImpl;

public class STTApplication implements ContinueActionHandler {
	private final Stage stage;
	@FXML
	TextArea commandText;
	@FXML
	ListView<TimeTrackingItem> history;
	@FXML
	ListView<String> searchView;

	private final ExecutorService executorService;

	private final CommandHandler commandHandler;
	private final ItemReader historySource;
	private final ReportWindowBuilder reportWindowBuilder;
	private final CommentSearcher searcher;

	public STTApplication(Stage stage, CommandHandler commandHandler,
			ItemReader historySource, ExecutorService executorService,
			ReportWindowBuilder reportWindow, CommentSearcher searcher) {
		this.searcher = checkNotNull(searcher);
		this.reportWindowBuilder = checkNotNull(reportWindow);
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
				history.scrollTo(items.size() - 1);
			}

			@Override
			protected void failed() {
				getException().printStackTrace();
			}
		});
	}

	public void setupStage() {
		ResourceBundle localization = ResourceBundle
				.getBundle("org.stt.gui.Application");
		FXMLLoader loader = new FXMLLoader(getClass().getResource(
				"/org/stt/gui/jfx/MainWindow.fxml"), localization);
		loader.setController(this);

		BorderPane pane;
		try {
			pane = (BorderPane) loader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
								String textToSet = newItem.getComment().get();
								setCommandText(textToSet);
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

			private final Image continueImage = new Image("/Continue.png", 18,
					18, true, true);

			@Override
			public ListCell<TimeTrackingItem> call(
					ListView<TimeTrackingItem> arg0) {
				return new TimeTrackingItemCell(STTApplication.this,
						continueImage);
			}
		});

		setupSearchView();

		Scene scene = new Scene(pane);
		stage.setScene(scene);
		stage.setTitle(localization.getString("window.title"));

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent arg0) {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						shutdown();
					}
				});
			}
		});
		stage.show();
		commandText.requestFocus();
	}

	private void setCommandText(String textToSet) {
		commandText.setText(textToSet);
		commandText.positionCaret(commandText.getLength());
	}

	private void setupSearchView() {
		ListBinding<String> searchListBinding = new ListBinding<String>() {
			{
				bind(commandText.textProperty());
			}

			@Override
			protected ObservableList<String> computeValue() {
				if (commandText.getText().isEmpty()) {
					return FXCollections.emptyObservableList();
				}
				Collection<String> results = searcher
						.searchForComments(commandText.getText());
				ArrayList<String> resultsToUse = new ArrayList<>();
				for (String string : results) {
					resultsToUse.add(string);
					if (resultsToUse.size() == 5) {
						break;
					}
				}
				return FXCollections.observableList(resultsToUse);
			}
		};
		searchView.setItems(searchListBinding);
		searchView.prefHeightProperty().bind(
				searchListBinding.sizeProperty().multiply(26));
		searchView.minHeightProperty().bind(searchView.prefHeightProperty());
		searchView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		searchView.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<String>() {

					@Override
					public void changed(
							ObservableValue<? extends String> observable,
							String oldValue, String newValue) {
						if (newValue != null) {
							setCommandText(newValue);
							searchView.getSelectionModel().clearSelection();
						}
					}
				});
	}

	@FXML
	protected void showReportWindow() {
		try {
			reportWindowBuilder.setupStage();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	protected void done() {
		executeCommand();
		shutdown();
	}

	private void shutdown() {
		stage.close();
		executorService.shutdown();
		Platform.exit();
		// Required because txExit() above is just swallowed...
		PlatformImpl.tkExit();
		try {
			executorService.awaitTermination(1, TimeUnit.SECONDS);
			commandHandler.close();
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
		// System.exit(0);
	}

	@FXML
	protected void fin() {
		commandHandler.endCurrentItem();
		shutdown();
	}

	@FXML
	protected void onKeyPressed(KeyEvent event) {
		if (KeyCode.ENTER.equals(event.getCode()) && event.isControlDown()) {
			event.consume();
			done();
		}
		if (KeyCode.SPACE.equals(event.getCode()) && event.isControlDown()) {
			if (searchView.getItems().size() > 0) {
				setCommandText(searchView.getItems().get(0));
			}
			event.consume();
		}
	}

	protected void executeCommand() {
		if (!commandText.getText().trim().isEmpty()) {
			commandHandler.executeCommand(commandText.getText());
			clearCommand();
		}
	}

	private void clearCommand() {
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

	@Override
	public void continueItem(TimeTrackingItem item) {
		commandHandler.resumeGivenItem(item);
		shutdown();
	}

}
