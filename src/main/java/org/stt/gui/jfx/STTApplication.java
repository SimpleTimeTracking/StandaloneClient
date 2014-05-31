package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.stt.CommandHandler;

import com.google.inject.Inject;

public class STTApplication {
	private Stage stage;
	@FXML
	TextArea commandText;
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
	void onKeyReleased(KeyEvent event) {
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
