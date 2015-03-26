package org.stt;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.stt.config.BaseConfig;
import org.stt.config.CommandTextConfig;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.persistence.stt.STTFile;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by dante on 05.12.14.
 */
public class BaseModule extends AbstractModule {
    private static final Logger LOG = Logger.getLogger(BaseModule.class
            .getName());
    @Override
    protected void configure() {
        bind(CommandHandler.class).to(ToItemWriterCommandHandler.class);
        bind(ExecutorService.class).toInstance(Executors.newSingleThreadExecutor());
    }

    @Provides
    @Singleton
    @STTFile
    private File provideDatabaseFile(Configuration configuration) {
        return configuration.getSttFile();
    }

    @Provides
    @Singleton
    private TimeTrackingItemListConfig provideTimeTrackingItemListConfig(YamlConfigService yamlConfigService) {
        return yamlConfigService.getConfig().getTimeTrackingItemListConfig();
    }

    @Provides
    @Singleton
    private CommandTextConfig provideCommandTextConfig(YamlConfigService yamlConfigService) {
        return yamlConfigService.getConfig().getCommandText();
    }

    @Provides
    @Singleton
    @Named("itemLog")
    private PrintWriter getItemLogFile(Configuration configuration) throws FileNotFoundException, UnsupportedEncodingException {
        File file = configuration.getItemLogFile();
        if (file.getParentFile().mkdirs()) {
            LOG.info("Created directory " + file.getParentFile().getAbsolutePath());
        }
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8")),
                true);
    }

}
