package org.stt.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.stt.Service;
import org.stt.event.ShuttingDown;
import org.stt.event.TimePassedEvent;
import org.stt.gui.jfx.STTApplication;
import org.stt.gui.jfx.WorktimePaneBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UIMain extends Application {

    private static final Logger LOG = Logger.getLogger(UIMain.class
            .getName());

    private List<Service> servicesToShutdown = new CopyOnWriteArrayList<>();
    private STTApplication application;
    private MBassador eventBus;

    public static void main(String[] args) {
        LOG.info("START");
        Application.launch(UIMain.class, args);
    }

    @Override
    public void init() throws Exception {
        LOG.info("Starting STT in UI mode");

        LOG.info("Starting injector");
        UIApplication uiApplication = DaggerUIApplication.create();

        LOG.info("Setting up event bus");
        eventBus = uiApplication.eventBus();
        eventBus.subscribe(this);
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> eventBus.publish(new TimePassedEvent()));
            }
        }, 0, 1000);

        startService(uiApplication.configService());
        startService(uiApplication.backupCreator());
        startService(uiApplication.achievementService());
        startService(uiApplication.itemLogService());

        application = uiApplication.sttApplication();
        WorktimePaneBuilder worktimePaneBuilder = uiApplication.worktimePaneBuilder();
        eventBus.subscribe(worktimePaneBuilder);
        application.addAdditional(worktimePaneBuilder);
        LOG.info("init() done");
    }

    @Handler
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

    private void startService(Service serviceInstance) throws Exception {
        LOG.info("Starting " + serviceInstance.getClass().getSimpleName());
        serviceInstance.start();
        servicesToShutdown.add(serviceInstance);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            LOG.log(Level.SEVERE, "Uncaught exception", e);
            eventBus.publish(e);
        });
        LOG.info("Showing window");
        application.start(primaryStage);
    }
}
