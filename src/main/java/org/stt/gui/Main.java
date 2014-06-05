package org.stt.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import org.stt.CommandHandler;
import org.stt.ToItemWriterCommandHandler;
import org.stt.gui.jfx.STTApplication;
import org.stt.importer.DefaultItemExporter;
import org.stt.importer.DefaultItemImporter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class Main extends AbstractModule {
	private static final Logger LOG = Logger.getLogger(Main.class.getName());

	@Override
	protected void configure() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		LOG.setLevel(Level.FINEST);
		LOG.addHandler(handler);
		bind(ExecutorService.class).toProvider(new Provider<ExecutorService>() {
			@Override
			public ExecutorService get() {
				return Executors.newSingleThreadExecutor();
			}
		});
		File file = getSTTFile();
		try {
			DefaultItemExporter itemWriter = new DefaultItemExporter(
					new FileWriter(file));
			bind(CommandHandler.class).toInstance(
					new ToItemWriterCommandHandler(itemWriter));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		bind(ItemReader.class).annotatedWith(Names.named("dataSource"))
				.toProvider(new Provider<ItemReader>() {

					@Override
					public ItemReader get() {
						File file = getSTTFile();
						if (file.exists()) {
							try {
								return new DefaultItemImporter(new FileReader(
										file));
							} catch (FileNotFoundException e) {
								throw new RuntimeException(e);
							}
						} else {
							return new ItemReader() {
								@Override
								public void close() throws IOException {
								}

								@Override
								public Optional<TimeTrackingItem> read() {
									return Optional.<TimeTrackingItem> absent();
								}
							};
						}
					}

				});
	}

	private File getSTTFile() {
		return new File(System.getProperty("user.home"), ".stt");
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
