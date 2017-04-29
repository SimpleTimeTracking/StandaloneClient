package org.stt;

import dagger.Module;
import dagger.Provides;
import org.stt.config.BackupConfig;
import org.stt.config.ConfigRoot;
import org.stt.persistence.stt.STTFile;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
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
    static File provideDatabaseFile(ConfigRoot configuration,
                                    @Named("homePath") String homePath) {
        File sttFile = configuration.getSttFile().file(homePath);
        migrateSTT1FileIfExisting(sttFile.getParentFile(), sttFile);

        if (!sttFile.exists()) {
            try {
                sttFile.createNewFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return sttFile;
    }

    private static void migrateSTT1FileIfExisting(File oldFile, File newFile) {
        if (newFile.exists()) {
            LOG.severe("Both, old and new data file found. Using new one.");
            return;
        }
        if (oldFile.isDirectory() || !oldFile.exists()) {
            // Already migrated or not existing yet
            return;
        }
        LOG.info("Found STT v1 activities file, migrating to STT v2");
        File tempFile = new File(oldFile.getParentFile(), oldFile.getName() + ".bak");
        renameAndFailIfnotPossible(oldFile, tempFile, String.format("Couldn't rename %s to %s.", oldFile.getAbsolutePath(), tempFile.getAbsolutePath()));
        File parentDir = newFile.getParentFile();
        boolean createdParentDir = parentDir.mkdirs();
        if (!createdParentDir) {
            renameAndFailIfnotPossible(tempFile, oldFile, String.format("Couldn't create parent %s. Also, couldn't rename %s back to %s - sorry.",
                    parentDir.getAbsolutePath(), tempFile.getAbsolutePath(), oldFile.getAbsolutePath()));
            throw new RuntimeException(String.format("Couldn't create parent %s.", parentDir.getAbsolutePath()));
        }

        renameAndFailIfnotPossible(tempFile, newFile, String.format("Couldn't rename to %s.", newFile.getAbsolutePath()));
    }

    private static void renameAndFailIfnotPossible(File fromFile, File toFile, String message) {
        boolean revertedRename = fromFile.renameTo(toFile);
        if (!revertedRename) {
            throw new RuntimeException(message);
        }
    }

    @Provides
    @Singleton
    @Named("itemLog")
    static PrintWriter getItemLogFile(BackupConfig configuration,
                                      @Named("homePath") String homePath) {
        File file = configuration.getItemLogFile().file(homePath);
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

    @Provides
    @Named("applicationMetadata")
    static Properties provideApplicationMetadata() {
        Properties properties = new Properties();
        try (InputStream in = BaseModule.class.getResourceAsStream("/version.info")) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }

    @Provides
    @Named("version")
    static String provideVersion(@Named("applicationMetadata") Properties applicationMetadata) {
        return applicationMetadata.getProperty("app.version");
    }

    @Provides
    @Named("release url")
    static URL provideReleaseURL(@Named("applicationMetadata") Properties applicationMetadata) {
        try {
            return new URL(applicationMetadata.getProperty("release.url"));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }
}
