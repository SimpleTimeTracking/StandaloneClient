package org.stt.gui;

import com.google.common.base.Optional;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.stt.CommandHandler;
import org.stt.Configuration;
import org.stt.Factory;
import org.stt.Singleton;
import org.stt.ToItemWriterCommandHandler;
import org.stt.gui.jfx.ReportWindowBuilder;
import org.stt.gui.jfx.STTApplication;
import org.stt.gui.jfx.STTApplication.Builder;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.CommonPrefixGrouper;
import org.stt.searching.DefaultItemSearcher;
import org.stt.searching.ItemSearcher;
import org.stt.stt.importer.STTItemPersister;
import org.stt.stt.importer.STTItemReader;
import org.stt.stt.importer.StreamResourceProvider;

public class MainContext {

	private static final Logger LOG = Logger.getLogger(MainContext.class
			.getName());
	private final Configuration configuration;

	private final Factory<Stage> stageFactory = new Factory<Stage>() {

		@Override
		public Stage create() {
			return new Stage();
		}
	};

	private final Factory<CommonPrefixGrouper> commonPrefixGrouper = new Factory<CommonPrefixGrouper>() {

		@Override
		public CommonPrefixGrouper create() {
			CommonPrefixGrouper commonPrefixGrouper = new CommonPrefixGrouper();

			try (ItemReader itemReader = persistenceReader.create()) {
				commonPrefixGrouper.scanForGroups(itemReader);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return commonPrefixGrouper;
		}
	};

	private final Factory<ItemReader> persistenceReader = new Factory<ItemReader>() {
		@Override
		public ItemReader create() {
			File file = getSTTFile();
			if (file.exists()) {
				try {
					return new STTItemReader(new InputStreamReader(
							new FileInputStream(file), "UTF-8"));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				return new ItemReader() {
					@Override
					public void close() throws IOException {
					}

					@Override
					public Optional<TimeTrackingItem> read() {
						return Optional.<TimeTrackingItem>absent();
					}
				};
			}
		}
	};

	private final Factory<ItemReaderProvider> itemReaderProvider = new Singleton<ItemReaderProvider>() {
		@Override
		protected ItemReaderProvider createInstance() {
			return new ItemReaderProvider() {

				@Override
				public ItemReader provideReader() {
					return persistenceReader.create();
				}
			};
		}
	};

	private final Factory<ItemSearcher> itemSearcher = new Singleton<ItemSearcher>() {
		@Override
		protected ItemSearcher createInstance() {
			return new DefaultItemSearcher(itemReaderProvider.create());
		}
	};

	private final Factory<StreamResourceProvider> streamResourceProvider = new Singleton<StreamResourceProvider>() {
		@Override
		protected StreamResourceProvider createInstance() {
			return new StreamResourceProvider() {
				private OutputStreamWriter outputStreamWriter;
				private InputStreamReader inReader;
				private OutputStreamWriter appendingOutWriter;

				@Override
				public Writer provideTruncatingWriter() throws IOException {
					outputStreamWriter = new OutputStreamWriter(
							new FileOutputStream(getSTTFile(), false), "UTF-8");
					return outputStreamWriter;
				}

				@Override
				public Reader provideReader() throws IOException {
					inReader = new InputStreamReader(new FileInputStream(
							getSTTFile()), "UTF-8");
					return inReader;
				}

				@Override
				public Writer provideAppendingWriter() throws IOException {
					appendingOutWriter = new OutputStreamWriter(
							new FileOutputStream(getSTTFile(), true), "UTF-8");
					return appendingOutWriter;
				}

				@Override
				public void close() {
					IOUtils.closeQuietly(outputStreamWriter);
					IOUtils.closeQuietly(inReader);
					IOUtils.closeQuietly(appendingOutWriter);
				}
			};

		}
	};

	protected Factory<ItemPersister> itemWriter = new Factory<ItemPersister>() {

		@Override
		public ItemPersister create() {
			return new STTItemPersister(streamResourceProvider.create());
		}
	};

	private final Factory<CommandHandler> commandHandler = new Singleton<CommandHandler>() {

		@Override
		protected CommandHandler createInstance() {
			return new ToItemWriterCommandHandler(itemWriter.create(),
					itemSearcher.create());
		}
	};

	private final Factory<ResourceBundle> resourceBundle = new Singleton<ResourceBundle>() {

		@Override
		protected ResourceBundle createInstance() {
			return ResourceBundle
					.getBundle("org.stt.gui.Application");
		}
	};

	public MainContext() {
		configuration = new Configuration();
	}

	private File getSTTFile() {
		return configuration.getSttFile();
	}

	public static void main(String[] args) {
		Platform.setImplicitExit(false);
		initializeJFXToolkit();
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				MainContext main = new MainContext();
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
		Stage stage = stageFactory.create();
		application.start(stage);
	}

	private void setupLogging() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		LOG.setLevel(Level.FINEST);
		LOG.addHandler(handler);
	}

	STTApplication createSTTApplication() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		ReportWindowBuilder reportWindowBuilder = new ReportWindowBuilder(
				stageFactory, itemReaderProvider.create(),
				itemSearcher.create());
		Builder builder = new STTApplication.Builder();
		builder.commandHandler(commandHandler.create())
				.historySourceProvider(itemReaderProvider.create())
				.executorService(executorService)
				.reportWindowBuilder(reportWindowBuilder)
				.expansionProvider(commonPrefixGrouper.create())
				.resourceBundle(resourceBundle.create());
		return builder.build();
	}
}
