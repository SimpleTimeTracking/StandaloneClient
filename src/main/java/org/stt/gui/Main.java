package org.stt.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

import org.stt.CommandHandler;
import org.stt.ToItemWriterCommandHandler;
import org.stt.gui.jfx.STTApplication;
import org.stt.importer.DefaultItemExporter;
import org.stt.importer.DefaultItemImporter;
import org.stt.importer.StreamResourceProvider;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemSearcher;
import org.stt.persistence.ItemWriter;
import org.stt.searching.DefaultItemSearcher;

import com.google.common.base.Optional;

public class Main {
	private static final Logger LOG = Logger.getLogger(Main.class.getName());

	private ItemReader createPersistenceReader() {
		File file = getSTTFile();
		if (file.exists()) {
			try {
				return new DefaultItemImporter(new FileReader(file));
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

	private ItemSearcher createItemSearcher() {
		return new DefaultItemSearcher(new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				return createPersistenceReader();
			}
		});
	}

	private ItemWriter createPersistenceWriter() throws IOException {
		return new DefaultItemExporter(createPersistenceStreamSupport());
	}

	private StreamResourceProvider createPersistenceStreamSupport()
			throws IOException {
		StreamResourceProvider srp = new StreamResourceProvider() {

			@Override
			public Writer provideTruncatingWriter() throws IOException {
				return new FileWriter(getSTTFile(), false);
			}

			@Override
			public Reader provideReader() throws FileNotFoundException {
				return new FileReader(getSTTFile());
			}

			@Override
			public Writer provideAppendingWriter() throws IOException {
				return new FileWriter(getSTTFile(), true);
			}

			@Override
			public void close() {

			}
		};
		return srp;
	}

	private File getSTTFile() {
		return new File(System.getProperty("user.home"), ".stt");
	}

	public static void main(String[] args) {
		Platform.setImplicitExit(false);
		initializeJFXToolkit();
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				Main main = new Main();
				main.start();
			}
		});
	}

	private static void initializeJFXToolkit() {
		// Hack to initialize Toolkit:
		new JFXPanel();
	}

	void start() {
		setupLogging();

		STTApplication application = createSTTApplication();
		application.start();
	}

	private void setupLogging() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		LOG.setLevel(Level.FINEST);
		LOG.addHandler(handler);
	}

	STTApplication createSTTApplication() {
		Stage stage = new Stage();
		CommandHandler commandHandler;
		try {
			commandHandler = new ToItemWriterCommandHandler(
					createPersistenceWriter(), createItemSearcher());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ItemReader historyReader = createPersistenceReader();
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		return new STTApplication(stage, commandHandler, historyReader,
				executorService);
	}
}
