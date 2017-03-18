package org.stt.persistence.stt;

import dagger.BindsOptionalOf;
import dagger.Module;
import dagger.Provides;
import net.engio.mbassy.bus.MBassador;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemWriter;

import javax.inject.Provider;
import java.io.*;

/**
 * Created by dante on 03.12.14.
 */
@Module
public abstract class STTPersistenceModule {
    private STTPersistenceModule() {
    }

    @Provides
    static ItemReader provideItemReader(@STTFile Reader reader) {
        return new STTItemReader(reader);
    }

    @Provides
    static ItemWriter provideItemWriter(@STTFile Writer writer) {
        return new STTItemWriter(writer);
    }

    @Provides
    static ItemPersister provideItemPersister(@STTFile Provider<Reader> readerProvider, @STTFile Provider<Writer> writerProvider) {
        return new STTItemPersister(readerProvider, writerProvider);
    }

    @BindsOptionalOf
    abstract MBassador<Object> optionalMBassador();

    @Provides
    @STTFile
    static Reader provideReader(@STTFile File sttFile) {
        try {
            return new InputStreamReader(
                    new FileInputStream(sttFile), "UTF-8");
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Provides
    @STTFile
    static Writer provideWriter(@STTFile File sttFile) {
        try {
            return new OutputStreamWriter(new FileOutputStream(sttFile, false), "UTF-8");
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

}
