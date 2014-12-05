package org.stt;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.stt.persistence.stt.STTFile;

import java.io.File;

/**
 * Created by dante on 05.12.14.
 */
public class BaseModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CommandHandler.class).to(ToItemWriterCommandHandler.class);
    }

    @Provides @Singleton @STTFile
    private File provideDatabasefile(Configuration configuration) {
        return configuration.getSttFile();
    }
}
