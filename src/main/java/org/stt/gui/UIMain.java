package org.stt.gui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.stt.BaseModule;
import org.stt.I18NModule;
import org.stt.Service;
import org.stt.YamlConfigService;
import org.stt.analysis.AnalysisModule;
import org.stt.event.EventBusModule;
import org.stt.event.ItemReaderService;
import org.stt.event.ShutdownRequest;
import org.stt.event.messages.ReadItemsRequest;
import org.stt.fun.AchievementModule;
import org.stt.gui.jfx.JFXModule;
import org.stt.gui.jfx.STTApplication;
import org.stt.persistence.BackupCreator;
import org.stt.persistence.stt.STTPersistenceModule;
import org.stt.time.TimeUtilModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UIMain {

    private static final Logger LOG = Logger.getLogger(UIMain.class
            .getName());

    private List<Service> servicesToShutdown = new ArrayList<>();

    public static void main(String[] args) {
        LOG.info("START");
        try {
            new UIMain().start();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static void initializeJFXToolkit() {
        // Hack to initialize Toolkit:
        new JFXPanel();
    }

    void start() throws Exception {
        final Injector injector = Guice.createInjector(new TimeUtilModule(), new STTPersistenceModule(), new I18NModule(), new EventBusModule(), new AchievementModule(), new AnalysisModule(),
                new JFXModule(), new BaseModule());

        EventBus eventBus = injector.getInstance(EventBus.class);
        eventBus.register(this);

        startService(injector, YamlConfigService.class);
        startService(injector, BackupCreator.class);
        startService(injector, ItemReaderService.class);

        final STTApplication application = injector.getInstance(STTApplication.class);
        LOG.info("Starting main application window");
        Platform.setImplicitExit(false);
        initializeJFXToolkit();
        LOG.info("Showing window");
        application.start();

        // Post initial request to load all items
        eventBus.post(new ReadItemsRequest());
    }

    @Subscribe
    public void shutdown(ShutdownRequest request) {
        LOG.info("Shutting down");
        Collections.reverse(servicesToShutdown);
        for (Service service : servicesToShutdown) {
            LOG.info("Stopping " + service.getClass().getSimpleName());
            service.stop();
        }
        System.exit(0);
    }

    private void startService(Injector injector, Class<? extends Service> service) throws Exception {
        Service serviceInstance = injector.getInstance(service);
        LOG.info("Starting " + serviceInstance.getClass().getSimpleName());
        serviceInstance.start();
        servicesToShutdown.add(serviceInstance);
    }
}
