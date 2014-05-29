package org.stt.gui;

import java.io.IOException;
import java.util.Properties;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import org.stt.gui.jfx.STTApplication;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class Main extends AbstractModule {

	@Override
	protected void configure() {
		Properties localization = new Properties();
		try {
			localization.load(getClass().getResourceAsStream(
					"/org/stt/gui/Application.properties"));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		Names.bindProperties(binder(), localization);
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
