package org.stt.persistence

import org.stt.Service
import org.stt.config.BackupConfig
import org.stt.persistence.stt.STTFile
import org.stt.time.DateTimes
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.*
import java.util.Objects.requireNonNull
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.streams.toList

/**
 * creates backups of the .stt file in configurable intervals and locations.
 * Optionally deletes old backup files if configured.
 */
@Singleton
class BackupCreator @Inject
constructor(backupConfig: BackupConfig,
            @STTFile sttFile: File,
            @Named("homePath") homePath: String) : Service {

    private val backupConfig: BackupConfig
    private val sttFile: File
    private val homePath: String

    init {
        this.backupConfig = requireNonNull(backupConfig)
        this.sttFile = requireNonNull(sttFile)
        this.homePath = requireNonNull(homePath)
    }

    override fun stop() {
        // No default behavior
    }

    /**
     * Perform the backup:
     *
     *  * check if backup is needed
     *
     *  * if so, copy the current .stt file to the backup location
     */
    @Throws(IOException::class)
    override fun start() {
        val backupInterval = backupConfig.backupInterval

        if (backupInterval < 1) {
            LOG.info("Backup is disabled (see backupInterval setting).")
            return
        }

        val backupLocation = backupConfig.backupLocation.file(homePath)

        if (!backupLocation.exists()) {
            val mkdirs = backupLocation.mkdirs()
            if (!mkdirs) {
                throw IOException("Couldn't create backup folder: " + backupLocation.absolutePath)
            }
        }

        if (!backupLocation.canWrite()) {
            throw IOException("cannot persist to " + backupLocation.absolutePath)
        }

        val sttFileName = sttFile.name

        val backedUpFiles: Collection<File> =
                Files.list(backupLocation.toPath())
                        .filter { path -> path.fileName.toString().matches("$sttFileName-[0-9]{4}-[0-9]{2}-[0-9]{2}".toRegex()) }
                        .map { it.toFile() }
                        .use { backupedFileStream -> backupedFileStream.toList() }

        if (backupNeeded(backedUpFiles, backupInterval, sttFile, backupLocation)) {

            val backupFileName = getBackupFileName(sttFile, LocalDate.now())
            val newBackupFile = File(backupLocation, backupFileName)
            Files.copy(sttFile.toPath(), newBackupFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }

        deleteOldBackupFiles(backedUpFiles)
    }

    /**
     * Deletes the oldest files (by filename) of the given Collection but keeps
     * the newest ones (configured by retentionCount)
     */
    private fun deleteOldBackupFiles(backedUpFiles: Collection<File>) {

        val retentionCount = backupConfig.backupRetentionCount
        if (retentionCount < 1) {
            // no deletion of old files desired by backupConfig
            return
        }

        val backupList = ArrayList(backedUpFiles)
        Collections.sort(backupList)
        Collections.reverse(backupList)
        for (i in backupList.indices) {
            if (i >= retentionCount) {
                val oldBackupFile = backupList[i]
                val success = oldBackupFile.delete()
                LOG.info {
                    String.format("deleting old backup file %s because of configured retention count. Deleted successfully? %s",
                            oldBackupFile.absolutePath, success)
                }
            }
        }
    }

    /**
     * @return if in the given Collection any File has a name
     */
    private fun backupNeeded(backedUpFiles: Collection<File>,
                             backupInterval: Int, sttFile: File, backupLocation: File): Boolean {

        val existingAbsoluteBackupFiles = backedUpFiles
                .map { it.absoluteFile }
                .toSet()
        for (i in 0 until backupInterval) {
            val backupFile = File(backupLocation, getBackupFileName(
                    sttFile, LocalDate.now().minusDays(i.toLong()))).absoluteFile

            // files are only equal if getAbsoluteFile is called
            if (existingAbsoluteBackupFiles.contains(backupFile)) {
                return false
            }
        }
        return true
    }

    /**
     * @return the name of the backup file for the given date
     */
    private fun getBackupFileName(sttFile: File, date: LocalDate): String {
        return sttFile.name + "-" + DateTimes.prettyPrintDate(date)
    }

    companion object {

        private val LOG = Logger.getLogger(BackupCreator::class.java.name)
    }
}
