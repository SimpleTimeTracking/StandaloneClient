package org.stt.persistence.stt;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import org.stt.persistence.*;
import org.stt.query.DefaultTimeTrackingItemQueries;
import org.stt.query.TimeTrackingItemQueries;

import java.io.*;

/**
 * Created by dante on 03.12.14.
 */
public class STTPersistenceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ItemReader.class).to(STTItemReader.class);
        bind(ItemWriter.class).to(STTItemWriter.class);
        bind(ItemReaderProvider.class).to(PreCachingItemReaderProvider.class);
        bind(TimeTrackingItemQueries.class).to(DefaultTimeTrackingItemQueries.class);
        bind(ItemPersister.class).to(STTItemPersister.class);
    }

    @Provides @Named("uncached")
    ItemReaderProvider directReaderProvider(final Provider<ItemReader> readerProvider) {
        return new ItemReaderProvider() {
            @Override
            public ItemReader provideReader() {
                return readerProvider.get();
            }
        };
    }

    @Provides @STTFile
    Reader provideReader(@STTFile File sttFile) throws FileNotFoundException, UnsupportedEncodingException {
        return new InputStreamReader(
                new FileInputStream(sttFile), "UTF-8");
    }

    @Provides @STTFile
    Writer provideWriter(@STTFile File sttFile) throws FileNotFoundException, UnsupportedEncodingException {
        return new OutputStreamWriter(new FileOutputStream(sttFile, false), "UTF-8");
    }

}
