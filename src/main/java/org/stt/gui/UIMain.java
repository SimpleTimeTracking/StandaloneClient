package org.stt.gui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.stage.Stage;
import org.stt.BaseModule;
import org.stt.I18NModule;
import org.stt.Service;
import org.stt.YamlConfigService;
import org.stt.analysis.AnalysisModule;
import org.stt.event.EventBusModule;
import org.stt.event.ItemLogService;
import org.stt.event.ShutdownRequest;
import org.stt.fun.AchievementModule;
import org.stt.fun.AchievementService;
import org.stt.gui.jfx.JFXModule;
import org.stt.gui.jfx.STTApplication;
import org.stt.persistence.BackupCreator;
import org.stt.persistence.PreCachingItemReaderProvider;
import org.stt.persistence.stt.STTPersistenceModule;
import org.stt.time.TimeUtilModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class UIMain extends Application {

    private static final Logger LOG = Logger.getLogger(UIMain.class
            .getName());

    private List<Service> servicesToShutdown = new ArrayList<>();
    private STTApplication application;

    public static void main(String[] args) {
        LOG.info("START");
        Application.launch( UIMain.class, args );
    }

    @Override
    public void init() throws Exception {
        super.init();
        LOG.info("Starting main application window");

        final Injector injector = Guice.createInjector(new TimeUtilModule(), new STTPersistenceModule(), new I18NModule(), new EventBusModule(), new AchievementModule(), new AnalysisModule(),
                new JFXModule(), new BaseModule());

        EventBus eventBus = injector.getInstance(EventBus.class);
        eventBus.register(this);
        eventBus.register(injector.getInstance(PreCachingItemReaderProvider.class));

        startService(injector, YamlConfigService.class);
        startService(injector, BackupCreator.class);
        startService(injector, AchievementService.class);
        startService(injector, ItemLogService.class);

        application = injector.getInstance(STTApplication.class);
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

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOG.info("Showing window");
        application.start( primaryStage );
    }
}
