package org.stt.persistence;

import org.stt.Service;
import org.stt.config.BackupConfig;
import org.stt.persistence.stt.STTFile;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * creates backups of the .stt file in configurable intervals and locations.
 * Optionally deletes old backup files if configured.
 */
@Singleton
public class BackupCreator implements Service {

    private static final Logger LOG = Logger.getLogger(BackupCreator.class.getName());

	private final BackupConfig backupConfig;
	private final File sttFile;
	private String homePath;

	@Inject
	public BackupCreator(BackupConfig backupConfig,
						 @STTFile File sttFile,
						 @Named("homePath") String homePath) {
		this.backupConfig = requireNonNull(backupConfig);
		this.sttFile = requireNonNull(sttFile);
		this.homePath = requireNonNull(homePath);
	}

	@Override
	public void stop() {
        // No default behavior
    }

	/**
	 * Perform the backup:
	 *
	 * <li>check if backup is needed
	 *
	 * <li>if so, copy the current .stt file to the backup location
	 */
	@Override
	public void start() throws IOException {
		int backupInterval = backupConfig.getBackupInterval();

		if (backupInterval < 1) {
			LOG.info("Backup is disabled (see backupInterval setting).");
			return;
		}

		File backupLocation = backupConfig.getBackupLocation().file(homePath);

        if (!backupLocation.exists()) {
            boolean mkdirs = backupLocation.mkdirs();
            if (!mkdirs) {
                throw new IOException("Couldn't create backup folder: " + backupLocation.getAbsolutePath());
            }
        }

		if (!backupLocation.canWrite()) {
            throw new IOException("cannot persist to "
                    + backupLocation.getAbsolutePath());
        }

		String sttFileName = sttFile.getName();

        Collection<File> backedUpFiles = Files.list(backupLocation.toPath())
                .filter(path -> path.getFileName().toString().matches(sttFileName
                        + "-[0-9]{4}-[0-9]{2}-[0-9]{2}"))
                .map(Path::toFile)
                .collect(Collectors.toList());

		if (backupNeeded(backedUpFiles, backupInterval, sttFile, backupLocation)) {

            String backupFileName = getBackupFileName(sttFile, LocalDate.now());
            File newBackupFile = new File(backupLocation, backupFileName);
            Files.copy(sttFile.toPath(), newBackupFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        }

		deleteOldBackupFiles(backedUpFiles);
	}

	/**
	 * Deletes the oldest files (by filename) of the given Collection but keeps
	 * the newest ones (configured by retentionCount)
	 */
	private void deleteOldBackupFiles(Collection<File> backedUpFiles) {

		int retentionCount = backupConfig.getBackupRetentionCount();
		if (retentionCount < 1) {
			// no deletion of old files desired by backupConfig
			return;
		}

		List<File> backupList = new ArrayList<>(backedUpFiles);
		Collections.sort(backupList);
		Collections.reverse(backupList);
		for (int i = 0; i < backupList.size(); i++) {
			if (i >= retentionCount) {
				boolean success = backupList.get(i).delete();
                LOG.info(String.format("deleting old backup file %s because of configured retention count. Deleted successfully? %s",
                        backupList.get(i).getAbsolutePath(), success));
            }
        }
    }

	/**
	 * @return if in the given Collection any File has a name
	 */
	private boolean backupNeeded(Collection<File> backedUpFiles,
			int backupInterval, File sttFile, File backupLocation) {

		for (int i = 0; i < backupInterval; i++) {

			File backupFile = new File(backupLocation, getBackupFileName(
                    sttFile, LocalDate.now().minusDays(i))).getAbsoluteFile();

			// files are only equal if getAbsoluteFile is called
			for (File currentFile : backedUpFiles) {
				if (currentFile.getAbsoluteFile().equals(backupFile)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
     * @return the name of the backup file for the given date
     */
    private String getBackupFileName(File sttFile, LocalDate date) {
        return sttFile.getName() + "-" + DateTimes.prettyPrintDate(date);
    }
}
