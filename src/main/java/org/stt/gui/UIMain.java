package org.stt.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.controlsfx.dialog.ExceptionDialog;
import org.stt.Service;
import org.stt.StopWatch;
import org.stt.event.ShuttingDown;
import org.stt.event.TimePassedEvent;
import org.stt.gui.jfx.MainWindowController;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UIMain extends Application {
    /**
     * Set to true to add debug graphics
     */
    public static final boolean DEBUG_UI = false;
    private static final Logger LOG = Logger.getLogger(UIMain.class
            .getName());

    private List<Service> servicesToShutdown = new CopyOnWriteArrayList<>();
    private MBassador<Object> eventBus;
    private MainWindowController mainWindowController;
    private ExecutorService executorService;

    public static void main(String[] args) {
        LOG.info("START");
        Application.launch(UIMain.class, args);
    }

    @Override
    public void init() throws Exception {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.getHandlers()[0].setLevel(Level.FINEST);
        Logger.getLogger("org.stt").setLevel(Level.FINEST);
        LOG.info("Starting STT in UI mode");

        LOG.info("Starting injector");
        UIApplication uiApplication = DaggerUIApplication.create();

        executorService = uiApplication.executorService();
        startEventBus(uiApplication);

        startService(uiApplication.configService());
        startService(uiApplication.backupCreator());
        startService(uiApplication.itemLogService());

        LOG.info("init() done");
        mainWindowController = uiApplication.mainWindow();
    }

    private void startEventBus(UIApplication uiApplication) {
        LOG.info("Setting up event bus");
        eventBus = uiApplication.eventBus();
        eventBus.subscribe(this);
    }

    @Handler(priority = -999)
    public void shutdown(ShuttingDown request) {
        LOG.info("Shutting down");
        try {
            Collections.reverse(servicesToShutdown);
            for (Service service : servicesToShutdown) {
                LOG.info("Stopping " + service.getClass().getSimpleName());
                service.stop();
            }
            executorService.shutdown();
        } finally {
            Platform.exit();
        }
    }

    private void startService(Service serviceInstance) throws Exception {
        StopWatch stopWatch = new StopWatch(serviceInstance.getClass().getSimpleName());
        LOG.info("Starting " + serviceInstance.getClass().getSimpleName());
        serviceInstance.start();
        servicesToShutdown.add(serviceInstance);
        stopWatch.stop();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            LOG.log(Level.SEVERE, "Uncaught exception", e);
            eventBus.publish(e);
            new ExceptionDialog(e).show();
        });
        LOG.info("Showing window");
        mainWindowController.show(primaryStage);

        scheduleOneUpdatePerSecond();
        LOG.fine("Window is now shown");
    }

    private void scheduleOneUpdatePerSecond() {
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> eventBus.publish(new TimePassedEvent()));
            }
        }, 0, 1000);
    }
}
