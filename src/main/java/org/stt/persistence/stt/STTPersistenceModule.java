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
import java.nio.charset.StandardCharsets;

@Module
public abstract class STTPersistenceModule { // NOSONAR Dagger requirement
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
        FileInputStream in;
        try {
            in = new FileInputStream(sttFile);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
        return new InputStreamReader(
                in, StandardCharsets.UTF_8);
    }

    @Provides
    @STTFile
    static Writer provideWriter(@STTFile File sttFile) {
        FileOutputStream out;
        try {
            out = new FileOutputStream(sttFile, false);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
        return new OutputStreamWriter(out, StandardCharsets.UTF_8);
    }

}
