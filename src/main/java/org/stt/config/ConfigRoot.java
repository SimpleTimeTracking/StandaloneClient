package org.stt.config;

public class ConfigRoot {
    private TimeTrackingItemListConfig timeTrackingItemListConfig = new TimeTrackingItemListConfig();
    private ReportWindowConfig reportWindowConfig = new ReportWindowConfig();
    private CommonPrefixGrouperConfig prefixGrouper = new CommonPrefixGrouperConfig();
    private CommandTextConfig commandText = new CommandTextConfig();

    public TimeTrackingItemListConfig getTimeTrackingItemListConfig() {
        return timeTrackingItemListConfig;
    }

    public void setTimeTrackingItemListConfig(
            TimeTrackingItemListConfig timeTrackingItemListConfig) {
        this.timeTrackingItemListConfig = timeTrackingItemListConfig;
    }

    public ReportWindowConfig getReportWindowConfig() {
        return reportWindowConfig;
    }

    public void setReportWindowConfig(ReportWindowConfig reportWindowConfig) {
        this.reportWindowConfig = reportWindowConfig;
    }

    public CommonPrefixGrouperConfig getPrefixGrouper() {
        return prefixGrouper;
    }

    public void setPrefixGrouper(CommonPrefixGrouperConfig prefixGrouper) {
        this.prefixGrouper = prefixGrouper;
    }

    public CommandTextConfig getCommandText() {
        return commandText;
    }

    public void setCommandText(CommandTextConfig commandText) {
        this.commandText = commandText;
    }
}
