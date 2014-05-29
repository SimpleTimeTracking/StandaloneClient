package org.stt.gui.jfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class STTApplication extends Application {
	private String windowTitle;

	private Stage stage;

	@Inject
	public void setWindowTitle(@Named("window.title") String windowTitle) {
		this.windowTitle = windowTitle;
	}

	@Inject
	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void start(Stage stage) throws Exception {
		stage.setTitle(windowTitle);
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
