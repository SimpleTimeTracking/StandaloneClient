package org.stt.config;

public class CliConfig implements ConfigurationContainer {
    private int cliReportingWidth = 80;
    private String systemOutEncoding = "UTF-8";

    public int getCliReportingWidth() {
        return cliReportingWidth;
    }

    public void setCliReportingWidth(int cliReportingWidth) {
        this.cliReportingWidth = cliReportingWidth;
    }

    public String getSystemOutEncoding() {
        return systemOutEncoding;
    }

    public void setSystemOutEncoding(String systemOutEncoding) {
        this.systemOutEncoding = systemOutEncoding;
    }
}
