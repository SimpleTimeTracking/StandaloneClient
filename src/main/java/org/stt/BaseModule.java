package org.stt;

import dagger.Module;
import dagger.Provides;
import org.stt.persistence.stt.STTFile;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Module
public class BaseModule {
    private static final Logger LOG = Logger.getLogger(BaseModule.class
            .getName());

    private BaseModule() {
    }

    @Provides
    @Singleton
    static ExecutorService provideExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Singleton
    @STTFile
    static File provideDatabaseFile(Configuration configuration) {
        return configuration.getSttFile();
    }

    @Provides
    @Singleton
    @Named("itemLog")
    static PrintWriter getItemLogFile(Configuration configuration) {
        File file = configuration.getItemLogFile();
        if (file.getParentFile().mkdirs()) {
            LOG.info("Created directory " + file.getParentFile().getAbsolutePath());
        }
        try {
            return new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8")),
                    true);
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

}
