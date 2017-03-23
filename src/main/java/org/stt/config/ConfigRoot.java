package org.stt.config;

public class ConfigRoot implements ConfigurationContainer {
    private ActivitiesConfig activities = new ActivitiesConfig();
    private CommonPrefixGrouperConfig prefixGrouper = new CommonPrefixGrouperConfig();
    private WorktimeConfig worktime = new WorktimeConfig();
    private ReportConfig report = new ReportConfig();
    private BackupConfig backup = new BackupConfig();
    private PathSetting sttFile = new PathSetting("$HOME$/.stt/activities");
    private CliConfig cli = new CliConfig();
    private JiraConfig jira = new JiraConfig();

    public ActivitiesConfig getActivities() {
        return activities;
    }

    public void setActivities(
            ActivitiesConfig activities) {
        this.activities = activities;
    }

    public CommonPrefixGrouperConfig getPrefixGrouper() {
        return prefixGrouper;
    }

    public void setPrefixGrouper(CommonPrefixGrouperConfig prefixGrouper) {
        this.prefixGrouper = prefixGrouper;
    }

    public void setWorktime(WorktimeConfig worktime) {
        this.worktime = worktime;
    }

    public WorktimeConfig getWorktime() {
        return worktime;
    }

    public ReportConfig getReport() {
        return report;
    }

    public void setReport(ReportConfig report) {
        this.report = report;
    }

    public BackupConfig getBackup() {
        return backup;
    }

    public void setBackup(BackupConfig backup) {
        this.backup = backup;
    }

    public PathSetting getSttFile() {
        return sttFile;
    }

    public void setSttFile(PathSetting sttFile) {
        this.sttFile = sttFile;
    }

    public CliConfig getCli() {
        return cli;
    }

    public void setCli(CliConfig cli) {
        this.cli = cli;
    }

    public JiraConfig getJira() {
        return jira;
    }

    public void setJira(JiraConfig jira) {
        this.jira = jira;
    }
}
