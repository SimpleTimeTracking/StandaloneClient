package org.stt

import dagger.Module
import dagger.Provides
import org.stt.config.BackupConfig
import org.stt.config.ConfigRoot
import org.stt.persistence.stt.STTFile
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors
import java.util.logging.Logger
import javax.inject.Named
import javax.inject.Singleton

@Module
class BaseModule {
    private val LOG = Logger.getLogger(BaseModule::class.java.name)

    @Provides
    @Singleton
    fun provideExecutorService() = Executors.newSingleThreadExecutor()

    @Provides
    @Singleton
    @STTFile
    fun provideDatabaseFile(configuration: ConfigRoot,
                            @Named("homePath") homePath: String): File {
        val sttFile = configuration.sttFile.file(homePath)
        migrateSTT1FileIfExisting(sttFile.parentFile, sttFile)

        if (!sttFile.exists()) {
            try {
                val newFile = sttFile.createNewFile()
                if (!newFile) {
                    throw AssertionError("'activities' file claimed to exist and *not* exist!")
                }
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }

        }
        return sttFile
    }

    private fun migrateSTT1FileIfExisting(oldFile: File, newFile: File) {
        if (newFile.exists()) {
            LOG.severe("Both, old and new data file found. Using new one.")
            return
        }
        if (oldFile.isDirectory || !oldFile.exists()) {
            // Already migrated or not existing yet
            return
        }
        LOG.info("Found STT v1 activities file, migrating to STT v2")
        val tempFile = File(oldFile.parentFile, oldFile.name + ".bak")
        renameAndFailIfnotPossible(oldFile, tempFile, String.format("Couldn't rename %s to %s.", oldFile.absolutePath, tempFile.absolutePath))
        val parentDir = newFile.parentFile
        val createdParentDir = parentDir.mkdirs()
        if (!createdParentDir) {
            renameAndFailIfnotPossible(tempFile, oldFile, String.format("Couldn't create parent %s. Also, couldn't rename %s back to %s - sorry.",
                    parentDir.absolutePath, tempFile.absolutePath, oldFile.absolutePath))
            throw MigrationException(String.format("Couldn't create parent %s.", parentDir.absolutePath))
        }

        renameAndFailIfnotPossible(tempFile, newFile, String.format("Couldn't rename to %s.", newFile.absolutePath))
    }

    private fun renameAndFailIfnotPossible(fromFile: File, toFile: File, message: String) {
        val revertedRename = fromFile.renameTo(toFile)
        if (!revertedRename) {
            throw MigrationException(message)
        }
    }

    @Provides
    @Singleton
    @Named("itemLog")
    fun getItemLogFile(configuration: BackupConfig,
                       @Named("homePath") homePath: String): PrintWriter {
        val file = configuration.itemLogFile.file(homePath)
        if (file.parentFile.mkdirs()) {
            LOG.info("Created directory " + file.parentFile.absolutePath)
        }
        val out: FileOutputStream
        try {
            out = FileOutputStream(file, true)
        } catch (e: FileNotFoundException) {
            throw UncheckedIOException(e)
        }

        return PrintWriter(BufferedWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)),
                true)
    }

    @Provides
    @Named("applicationMetadata")
    fun provideApplicationMetadata(): Properties {
        val properties = Properties()
        BaseModule::class.java.getResourceAsStream("/version.info").use { properties.load(it) }
        return properties
    }

    @Provides
    @Named("version")
    fun provideVersion(@Named("applicationMetadata") applicationMetadata: Properties) =
            applicationMetadata.getProperty("app.version")

    @Provides
    @Named("release url")
    fun provideReleaseURL(@Named("applicationMetadata") applicationMetadata: Properties) =
            try {
                URL(applicationMetadata.getProperty("release.url"))
            } catch (e: MalformedURLException) {
                throw UncheckedIOException(e)
            }

    @Provides
    @Named("commit hash")
    fun provideCommitHash(@Named("applicationMetadata") applicationMetadata: Properties): String =
            applicationMetadata.getProperty("app.hash")

    class MigrationException internal constructor(message: String) : RuntimeException(message)
}
