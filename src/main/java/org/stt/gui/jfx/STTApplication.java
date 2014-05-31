package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import com.google.inject.Inject;

public class STTApplication extends Application {
	private Stage stage;

	@Inject
	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void start(Stage stage) throws Exception {
		checkNotNull(stage);

		ResourceBundle localization = ResourceBundle
				.getBundle("org.stt.gui.Application");
		BorderPane pane = FXMLLoader.load(
				getClass().getResource("/org/stt/gui/jfx/MainWindow.fxml"),
				localization);
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

	public void start() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					init();
					start(stage);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
