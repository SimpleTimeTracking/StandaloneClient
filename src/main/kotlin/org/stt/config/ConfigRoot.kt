package org.stt.config

class ConfigRoot : ConfigurationContainer {
    var activities = ActivitiesConfig()
    var prefixGrouper = CommonPrefixGrouperConfig()
    var worktime = WorktimeConfig()
    var report = ReportConfig()
    var backup = BackupConfig()
    var sttFile = PathSetting("\$HOME$/.stt/activities")
    var cli = CliConfig()
    var jira = JiraConfig()
}
