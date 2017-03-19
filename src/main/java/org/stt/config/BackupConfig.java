package org.stt.config;

public class BackupConfig implements ConfigurationContainer {
    private int backupInterval = 7;
    private int backupRetentionCount = 0;
    private PathSetting backupLocation = new PathSetting("$HOME$");
    private PathSetting itemLogFile = new PathSetting("$HOME$/.stt/itemlog");

    public int getBackupInterval() {
        return backupInterval;
    }

    public void setBackupInterval(int backupInterval) {
        this.backupInterval = backupInterval;
    }

    public PathSetting getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(PathSetting backupLocation) {
        this.backupLocation = backupLocation;
    }

    public int getBackupRetentionCount() {
        return backupRetentionCount;
    }

    public void setBackupRetentionCount(int backupRetentionCount) {
        this.backupRetentionCount = backupRetentionCount;
    }

    public PathSetting getItemLogFile() {
        return itemLogFile;
    }

    public void setItemLogFile(PathSetting itemLogFile) {
        this.itemLogFile = itemLogFile;
    }
}
