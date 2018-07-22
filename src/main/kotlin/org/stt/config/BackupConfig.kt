package org.stt.config

class BackupConfig : ConfigurationContainer {
    var backupInterval = 7
    var backupRetentionCount = 0
    var backupLocation = PathSetting("\$HOME$/.stt/backups")
    var itemLogFile = PathSetting("\$HOME$/.stt/itemlog")
}
