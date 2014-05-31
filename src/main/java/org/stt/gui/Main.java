package org.stt.gui;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import org.stt.CommandHandler;
import org.stt.gui.jfx.STTApplication;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main extends AbstractModule {
	private static final Logger LOG = Logger.getLogger(Main.class.getName());

	@Override
	protected void configure() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		LOG.setLevel(Level.FINEST);
		LOG.addHandler(handler);
		bind(CommandHandler.class).toInstance(new CommandHandler() {

			@Override
			public void executeCommand(String command) {
				LOG.fine(command);
			}
		});
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
