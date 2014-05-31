package org.stt.gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import org.stt.gui.jfx.STTApplication;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main extends AbstractModule {

	@Override
	protected void configure() {
	}

	public static void main(String[] args) {
		initializeJFXToolkit();
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				Main main = new Main();
				main.start(Guice.createInjector(new Main()));
			}
		});
	}

	private static void initializeJFXToolkit() {
		// Hack to initialize Toolkit:
		new JFXPanel();
	}

	void start(Injector injector) {
		STTApplication instance = injector.getInstance(STTApplication.class);
		instance.start();
	}
}
