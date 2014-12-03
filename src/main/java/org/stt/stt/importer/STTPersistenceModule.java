package org.stt.stt.importer;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.searching.DefaultItemSearcher;
import org.stt.searching.ItemSearcher;

import java.io.*;

/**
 * Created by dante on 03.12.14.
 */
public class STTPersistenceModule extends AbstractModule {
    @Override
    protected void configure() {
        final Provider<ItemReader> providerForItemReaders = getProvider(ItemReader.class);
        bind(ItemReader.class).to(STTItemReader.class);
        bind(ItemReaderProvider.class).toInstance(new ItemReaderProvider() {
            @Override
            public ItemReader provideReader() {
                return providerForItemReaders.get();
            }
        });
        bind(ItemSearcher.class).to(DefaultItemSearcher.class);
    }

    @Provides
    Reader provideReader(File sttFile) throws FileNotFoundException, UnsupportedEncodingException {
        return new InputStreamReader(
                new FileInputStream(sttFile), "UTF-8");
    }

}
