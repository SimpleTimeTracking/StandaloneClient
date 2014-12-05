package org.stt.persistence.stt;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemWriter;
import org.stt.search.DefaultItemSearcher;
import org.stt.search.ItemSearcher;

import java.io.*;

/**
 * Created by dante on 03.12.14.
 */
public class STTPersistenceModule extends AbstractModule {
    @Override
    protected void configure() {
        final Provider<ItemReader> providerForItemReaders = getProvider(ItemReader.class);
        bind(ItemReader.class).to(STTItemReader.class);
        bind(ItemWriter.class).to(STTItemWriter.class);
        bind(ItemReaderProvider.class).toInstance(new ItemReaderProvider() {
            @Override
            public ItemReader provideReader() {
                return providerForItemReaders.get();
            }
        });
        bind(ItemSearcher.class).to(DefaultItemSearcher.class);
        bind(ItemPersister.class).to(STTItemPersister.class);
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
