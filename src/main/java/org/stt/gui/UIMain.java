package org.stt.gui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.stt.BaseModule;
import org.stt.I18NModule;
import org.stt.Service;
import org.stt.config.YamlConfigService;
import org.stt.config.ConfigModule;
import org.stt.connector.jira.JiraConnector;
import org.stt.text.TextModule;
import org.stt.event.EventBusModule;
import org.stt.event.ItemLogService;
import org.stt.event.ShuttingDown;
import org.stt.event.TimePassedEvent;
import org.stt.fun.AchievementModule;
import org.stt.fun.AchievementService;
import org.stt.gui.jfx.JFXModule;
import org.stt.gui.jfx.STTApplication;
import org.stt.gui.jfx.WorktimePaneBuilder;
import org.stt.persistence.BackupCreator;
import org.stt.persistence.PreCachingItemReaderProvider;
import org.stt.persistence.stt.STTPersistenceModule;
import org.stt.time.TimeUtilModule;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class UIMain extends Application {

    private static final Logger LOG = Logger.getLogger(UIMain.class
            .getName());

    private List<Service> servicesToShutdown = new CopyOnWriteArrayList<>();
    private STTApplication application;
    private EventBus eventBus;

    public static void main(String[] args) {
        LOG.info("START");
        Application.launch(UIMain.class, args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        LOG.info("Starting STT in UI mode");

        LOG.info("Starting injector");
        final Injector injector = Guice.createInjector(new TimeUtilModule(), new STTPersistenceModule(), new I18NModule(), new EventBusModule(), new AchievementModule(), new TextModule(),
                new JFXModule(), new BaseModule(), new ConfigModule());

        LOG.info("Setting up event bus");
        eventBus = injector.getInstance(EventBus.class);
        eventBus.register(this);
        eventBus.register(injector.getInstance(PreCachingItemReaderProvider.class));
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        eventBus.post(new TimePassedEvent());
                    }
                });
            }
        }, 0, 1000);

        startService(injector, YamlConfigService.class);
        startService(injector, BackupCreator.class);
        startServiceInBackground(injector, AchievementService.class);
        startService(injector, ItemLogService.class);
        startService(injector, JiraConnector.class);
        
        application = injector.getInstance(STTApplication.class);
        WorktimePaneBuilder worktimePaneBuilder = injector.getInstance(WorktimePaneBuilder.class);
        eventBus.register(worktimePaneBuilder);
        application.addAdditional(worktimePaneBuilder);
        LOG.info("init() done");
    }

    @Subscribe
    public void shutdown(ShuttingDown request) {
        LOG.info("Shutting down");
        try {
            Collections.reverse(servicesToShutdown);
            for (Service service : servicesToShutdown) {
                LOG.info("Stopping " + service.getClass().getSimpleName());
                service.stop();
            }
        } finally {
            System.exit(0);
        }
    }

    private void startService(Injector injector, Class<? extends Service> service) throws Exception {
        Service serviceInstance = injector.getInstance(service);
        LOG.info("Starting " + serviceInstance.getClass().getSimpleName());
        serviceInstance.start();
        servicesToShutdown.add(serviceInstance);
    }

    private void startServiceInBackground(final Injector injector, final Class<? extends Service> service) {
        final ExecutorService executorService = injector.getInstance(ExecutorService.class);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    startService(injector, service);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error while starting service " + service.getName(), e);
                }
            }
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                eventBus.post(e);
            }
        });
        LOG.info("Showing window");
        application.start(primaryStage);
    }
}
