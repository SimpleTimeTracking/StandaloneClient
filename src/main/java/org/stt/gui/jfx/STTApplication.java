package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.stt.CommandHandler;
import org.stt.gui.jfx.ResultViewConfigurer.Callback;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;
import org.stt.searching.ExpansionProvider;

import com.sun.javafx.application.PlatformImpl;

public class STTApplication implements Callback {
	private static final Logger LOG = Logger.getLogger(STTApplication.class
			.getName());

	@FXML
	TextArea commandText;

	@FXML
	Button finButton;

	@FXML
	Button doneButton;

	@FXML
	ListView<TimeTrackingItem> result;

	private final Stage stage;
	private final ExecutorService executorService;
	private final CommandHandler commandHandler;
	private final ItemReader historySource;
	private final ReportWindowBuilder reportWindowBuilder;
	private final ExpansionProvider expansionProvider;

	final ObservableList<TimeTrackingItem> allItems = FXCollections
			.observableArrayList();

	private STTApplication(Builder builder) {
		this.expansionProvider = checkNotNull(builder.expansionProvider);
		this.reportWindowBuilder = checkNotNull(builder.reportWindowBuilder);
		this.stage = checkNotNull(builder.stage);
		this.commandHandler = checkNotNull(builder.commandHandler);
		this.historySource = checkNotNull(builder.historySource);
		this.executorService = checkNotNull(builder.executorService);
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

		setupResultView();

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

	private void setupResultView() {
		ResultViewConfigurer resultViewConfigurer = new ResultViewConfigurer();
		resultViewConfigurer.configure(result, allItems,
				commandText.textProperty(), this);
	}

	@Override
	public void textOfSelectedItem(String textToSet) {
		setCommandText(textToSet);
		requestFocusOnCommandText();
	}

	private void requestFocusOnCommandText() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				commandText.requestFocus();
			}
		});
	}

	private void setCommandText(String textToSet) {
		commandText.setText(textToSet);
		commandText.positionCaret(commandText.getLength());
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
		int caretPosition = commandText.getCaretPosition();
		String textToExpand = commandText.getText().substring(0, caretPosition);
		List<String> expansions = expansionProvider
				.getPossibleExpansions(textToExpand);
		if (!expansions.isEmpty()) {
			String maxExpansion = expansions.get(0);
			for (String exp : expansions) {
				maxExpansion = commonPrefix(maxExpansion, exp);
			}
			String tail = commandText.getText().substring(caretPosition);
			String expandedText = textToExpand + maxExpansion;
			commandText.setText(expandedText + tail);
			commandText.positionCaret(expandedText.length());
		}
	}

	String commonPrefix(String a, String b) {
		for (int i = 0; i < a.length() && i < b.length(); i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return a;
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

	public static class Builder {
		private Stage stage;
		private ExecutorService executorService;
		private CommandHandler commandHandler;
		private ItemReader historySource;
		private ReportWindowBuilder reportWindowBuilder;
		private ExpansionProvider expansionProvider;

		public Builder stage(Stage stage) {
			this.stage = stage;
			return this;
		}

		public Builder executorService(ExecutorService executorService) {
			this.executorService = executorService;
			return this;
		}

		public Builder commandHandler(CommandHandler commandHandler) {
			this.commandHandler = commandHandler;
			return this;
		}

		public Builder historySource(ItemReader historySource) {
			this.historySource = historySource;
			return this;
		}

		public Builder reportWindowBuilder(
				ReportWindowBuilder reportWindowBuilder) {
			this.reportWindowBuilder = reportWindowBuilder;
			return this;
		}

		public Builder expansionProvider(ExpansionProvider expansionProvider) {
			this.expansionProvider = expansionProvider;
			return this;
		}

		public STTApplication build() {
			return new STTApplication(this);
		}
	}
}
