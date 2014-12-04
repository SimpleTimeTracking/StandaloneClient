package org.stt.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.joda.time.DateTime;
import org.stt.Configuration;
import org.stt.time.DateTimeHelper;

/**
 * creates backups of the .stt file in configurable intervals and locations.
 * Optionally deletes old backup files if configured.
 */
@Singleton
public class BackupCreator {

	private static Logger LOG = Logger.getLogger(BackupCreator.class.getName());

	private final Configuration configuration;

	@Inject
	public BackupCreator(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Perform the backup:
	 * 
	 * <li>check if backup is needed
	 * 
	 * <li>if so, copy the current .stt file to the backup location
	 */
	public void performBackup() throws IOException {

		int backupInterval = configuration.getBackupInterval();

		if (backupInterval < 1) {
			// no backup desired by configuration
			return;
		}

		File backupLocation = configuration.getBackupLocation();

		if (!backupLocation.canWrite()) {
			throw new RuntimeException("cannot write to "
					+ backupLocation.getAbsolutePath());
		}

		File sttFile = configuration.getSttFile();
		String sttFileName = sttFile.getName();

		Collection<File> backedUpFiles = FileUtils
				.listFiles(backupLocation, new RegexFileFilter(sttFileName
						+ "-[0-9]{4}-[0-9]{2}-[0-9]{2}"), null);

		if (backupNeeded(backedUpFiles, backupInterval, sttFile, backupLocation)) {

			String backupFileName = getBackupFileName(sttFile, DateTime.now());
			File newBackupFile = new File(backupLocation, backupFileName);
			// to be safe we don't accidentally overwrite something: check if
			// there is already a file
			if (!newBackupFile.exists()) {
				FileUtils.copyFile(sttFile, newBackupFile);
			}
		}

		deleteOldBackupFiles(backedUpFiles);
	}

	/**
	 * Deletes the oldest files (by filename) of the given Collection but keeps
	 * the newest ones (configured by retentionCount)
	 */
	private void deleteOldBackupFiles(Collection<File> backedUpFiles) {

		int retentionCount = configuration.getBackupRetentionCount();
		if (retentionCount < 1) {
			// no deletion of old files desired by configuration
			return;
		}

		List<File> backupList = new ArrayList<>(backedUpFiles);
		Collections.sort(backupList);
		Collections.reverse(backupList);
		for (int i = 0; i < backupList.size(); i++) {
			if (i >= retentionCount) {
				boolean success = backupList.get(i).delete();
				LOG.info("deleting ond backup file "
						+ backupList.get(i).getAbsolutePath()
						+ " because of configured retention count. Deleted successfully? "
						+ success);
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
					sttFile, DateTime.now().minusDays(i))).getAbsoluteFile();

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
	 * @return the name of the backup file for the given {@link DateTime}
	 */
	private String getBackupFileName(File sttFile, DateTime date) {
		return sttFile.getName() + "-" + DateTimeHelper.prettyPrintDate(date);
	}
}
