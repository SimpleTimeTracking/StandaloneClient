package org.stt.persistence.stt

import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import net.engio.mbassy.bus.MBassador
import org.stt.persistence.ItemPersister
import org.stt.persistence.ItemReader
import org.stt.persistence.ItemWriter
import java.io.*
import java.nio.charset.StandardCharsets
import javax.inject.Provider

@Module
abstract class STTPersistenceModule {

    @BindsOptionalOf
    abstract fun optionalMBassador(): MBassador<Any>

    @Module
    companion object {
        @Provides
        @JvmStatic
        fun provideItemReader(@STTFile reader: Reader): ItemReader {
            return STTItemReader(reader)
        }

        @Provides
        @JvmStatic
        fun provideItemWriter(@STTFile writer: Writer): ItemWriter {
            return STTItemWriter(writer)
        }

        @Provides
        @JvmStatic
        fun provideItemPersister(@STTFile readerProvider: Provider<Reader>, @STTFile writerProvider: Provider<Writer>): ItemPersister {
            return STTItemPersister(readerProvider, writerProvider)
        }

        @Provides
        @JvmStatic
        @STTFile
        fun provideReader(@STTFile sttFile: File): Reader {
            val `in`: FileInputStream
            try {
                `in` = FileInputStream(sttFile)
            } catch (e: FileNotFoundException) {
                throw UncheckedIOException(e)
            }

            return InputStreamReader(
                    `in`, StandardCharsets.UTF_8)
        }

        @Provides
        @JvmStatic
        @STTFile
        fun provideWriter(@STTFile sttFile: File): Writer {
            val out: FileOutputStream
            try {
                out = FileOutputStream(sttFile, false)
            } catch (e: FileNotFoundException) {
                throw UncheckedIOException(e)
            }

            return OutputStreamWriter(out, StandardCharsets.UTF_8)
        }
    }

}
