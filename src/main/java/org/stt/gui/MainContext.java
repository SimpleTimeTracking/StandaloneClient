package org.stt.gui;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.joda.time.Duration;
import org.stt.*;
import org.stt.analysis.AnalysisModule;
import org.stt.event.EventBusModule;
import org.stt.event.ItemReaderService;
import org.stt.event.messages.ReadItemsRequest;
import org.stt.fun.AchievementModule;
import org.stt.gui.jfx.JFXModule;
import org.stt.gui.jfx.STTApplication;
import org.stt.persistence.BackupCreator;
import org.stt.persistence.stt.STTFile;
import org.stt.persistence.stt.STTPersistenceModule;
import org.stt.time.DateTimeHelper;
import org.stt.time.DurationRounder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainContext {

    private static final Logger LOG = Logger.getLogger(MainContext.class
            .getName());
    private final Configuration configuration;

    public MainContext() {
        configuration = new Configuration();
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

    private File getSTTFile() {
        return configuration.getSttFile();
    }

    void start() {
        setupLogging();
        Injector injector = Guice.createInjector(new AbstractModule() {
                                                     @Override
                                                     protected void configure() {
                                                         bind(CommandHandler.class).to(ToItemWriterCommandHandler.class);
                                                         bind(ExecutorService.class).toInstance(Executors.newSingleThreadExecutor());
                                                         bind(File.class).annotatedWith(STTFile.class).toInstance(getSTTFile());
                                                     }

                                                     @Provides
                                                     DurationRounder provideDurationRounder() {
                                                         DurationRounder rounder = new DurationRounder();
                                                         final Duration durationToRoundTo = configuration
                                                                 .getDurationToRoundTo();
                                                         rounder.setInterval(durationToRoundTo);
                                                         LOG.info("Rounding to "
                                                                 + DateTimeHelper.FORMATTER_PERIOD_H_M_S
                                                                 .print(durationToRoundTo.toPeriod()));
                                                         return rounder;
                                                     }
                                                 }, new STTPersistenceModule(), new I18NModule(), new EventBusModule(), new AchievementModule(), new AnalysisModule(),
                new JFXModule());
        // perform backup
        try {
            BackupCreator backupCreator = injector.getInstance(BackupCreator.class);
            backupCreator.performBackup();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        injector.getInstance(ItemReaderService.class);

        STTApplication application = injector.getInstance(STTApplication.class);
        Stage stage = injector.getInstance(Stage.class);
        application.start(stage);

        EventBus eventBus = injector.getInstance(EventBus.class);
        eventBus.post(new ReadItemsRequest());
    }

    private void setupLogging() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINEST);
        LOG.setLevel(Level.FINEST);
        LOG.addHandler(handler);
    }
}
