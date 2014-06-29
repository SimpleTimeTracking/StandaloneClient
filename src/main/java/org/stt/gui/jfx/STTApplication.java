package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javafx.scene.control.Button;
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
import org.stt.gui.jfx.TimeTrackingItemCell.Builder;
import org.stt.gui.jfx.TimeTrackingItemCell.ContinueActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.DeleteActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.EditActionHandler;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;
import org.stt.reporting.ItemGrouper;
import org.stt.searching.CommentSearcher;

import com.sun.javafx.application.PlatformImpl;

public class STTApplication implements ContinueActionHandler,
		EditActionHandler, DeleteActionHandler {
	private static final Logger LOG = Logger.getLogger(STTApplication.class
			.getName());

	private final Stage stage;
	@FXML
	TextArea commandText;

	@FXML
	Button finButton;

	@FXML
	Button doneButton;

	@FXML
	ListView<TimeTrackingItem> result;

	private final ExecutorService executorService;

	private final CommandHandler commandHandler;
	private final ItemReader historySource;
	private final ReportWindowBuilder reportWindowBuilder;
	private final CommentSearcher searcher;

	final ObservableList<TimeTrackingItem> allItems = FXCollections
			.observableArrayList();
	private final ItemGrouper grouper;

	public STTApplication(Stage stage, CommandHandler commandHandler,
			ItemReader historySource, ExecutorService executorService,
			ReportWindowBuilder reportWindow, CommentSearcher searcher,
			ItemGrouper grouper) {
		this.grouper = checkNotNull(grouper);
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
				allItems.setAll(getValue());
			}

			@Override
			protected void failed() {
				getException().printStackTrace();
			}
		});
	}

	public void setupStage() {
		final ResourceBundle localization = ResourceBundle
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

		result.setItems(createResultsListBinding());
		result.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		result.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<TimeTrackingItem>() {

					@Override
					public void changed(
							ObservableValue<? extends TimeTrackingItem> observable,
							TimeTrackingItem oldItem, TimeTrackingItem newItem) {
						if (newItem != null) {
							result.getSelectionModel().clearSelection();
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
		result.setCellFactory(new Callback<ListView<TimeTrackingItem>, ListCell<TimeTrackingItem>>() {
			private final Image deleteImage = new Image("/Delete.png", 25, 25,
					true, true);

			private final Image continueImage = new Image("/Continue.png", 25,
					25, true, true);

			private final Image editImage = new Image("/Edit.png", 25, 25,
					true, true);

			private final Image fromToImage = new Image("/FromTo.png", 32, 12,
					true, true);

			private final Image runningImage = new Image("Running.png", 32, 8,
					true, true);

			@Override
			public ListCell<TimeTrackingItem> call(
					ListView<TimeTrackingItem> arg0) {
				Builder builder = new TimeTrackingItemCell.Builder();
				builder.continueActionHandler(STTApplication.this)
						.deleteActionHandler(STTApplication.this)
						.editActionHandler(STTApplication.this)
						.continueImage(continueImage).deleteImage(deleteImage)
						.editImage(editImage).runningImage(runningImage)
						.fromToImage(fromToImage);
				return builder.build();
			}
		});

		setupSearchView();

		Scene scene = new Scene(pane);
		finButton.setMnemonicParsing(true);

		stage.setScene(scene);
		stage.setTitle(localization.getString("window.title"));
		Image applicationIcon = new Image("/Logo.png", 32, 32, true, true);
		stage.getIcons().add(applicationIcon);

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

	private ObservableList<TimeTrackingItem> createResultsListBinding() {
		return new ListBinding<TimeTrackingItem>() {
			{
				bind(allItems, commandText.textProperty());
			}

			@Override
			protected ObservableList<TimeTrackingItem> computeValue() {
				List<TimeTrackingItem> result;

				String command = commandText.textProperty().get().toLowerCase();
				if (command.isEmpty()) {
					result = new ArrayList<>(allItems);
				} else {
					result = new ArrayList<TimeTrackingItem>();
					for (TimeTrackingItem item : allItems) {
						if (item.getComment().isPresent()
								&& item.getComment().get().toLowerCase()
										.contains(command)) {
							result.add(item);
						}
					}
				}
				Collections.reverse(result);
				return FXCollections.observableList(result);
			}
		};
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
		searchListBinding.getClass(); // FIXME: just so findbugs is happy
		// searchView.setItems(searchListBinding);
		// searchView.prefHeightProperty().bind(
		// searchListBinding.sizeProperty().multiply(26));
		// searchView.minHeightProperty().bind(searchView.prefHeightProperty());
		// searchView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		// searchView.getSelectionModel().selectedItemProperty()
		// .addListener(new ChangeListener<String>() {
		//
		// @Override
		// public void changed(
		// ObservableValue<? extends String> observable,
		// String oldValue, String newValue) {
		// if (newValue != null) {
		// setCommandText(newValue);
		// searchView.getSelectionModel().clearSelection();
		// }
		// }
		// });
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
		if (KeyCode.ESCAPE.equals(event.getCode())) {
			event.consume();
			shutdown();
		}
		if (KeyCode.SPACE.equals(event.getCode()) && event.isControlDown()) {
			expandCurrentCommand();
			event.consume();
		}
	}

	void expandCurrentCommand() {
		String currentText = commandText.getText();
		List<String> expansions = grouper.getPossibleExpansions(currentText);
		if (expansions.size() == 1) {
			setCommandText(currentText + expansions.get(0));
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
					LOG.log(Level.SEVERE, "Couldn't start", e);
					shutdown();
				}
			}
		});
	}

	@Override
	public void continueItem(TimeTrackingItem item) {
		commandHandler.resumeGivenItem(item);
		shutdown();
	}

	@Override
	public void edit(TimeTrackingItem item) {
		setCommandText(commandHandler.itemToCommand(item));
	}

	@Override
	public void delete(TimeTrackingItem item) {
		try {
			commandHandler.delete(checkNotNull(item));
			allItems.remove(item);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
