package org.stt.config;


import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import java.io.File;

@Module
public class ConfigModule {
    private ConfigModule() {
    }

    @Provides
    static ConfigRoot provideConfigRoot(YamlConfigService configService) {
        return configService.getConfig();
    }

    @Provides
    static ActivitiesConfig provideTimeTrackingItemListConfig(ConfigRoot configRoot) {
        return configRoot.getActivities();
    }

    @Provides
    static WorktimeConfig provideWorktimeConfig(ConfigRoot configRoot) {
        return configRoot.getWorktime();
    }

    @Provides
    static ReportConfig provideReportConfig(ConfigRoot configRoot) {
        return configRoot.getReport();
    }

    @Provides
    static CliConfig provideCliConfig(ConfigRoot configRoot) {
        return configRoot.getCli();
    }

    @Provides
    static BackupConfig provideBackupConfig(ConfigRoot configRoot) {
        return configRoot.getBackup();
    }

    @Provides
    static CommonPrefixGrouperConfig provideCommonPrefixGrouperConfig(ConfigRoot configRoot) {
        return configRoot.getPrefixGrouper();
    }

    @Provides
    static JiraConfig provideJiraConfig(ConfigRoot configRoot) {
        return configRoot.getJira();
    }

    @Provides
    @Named("homePath")
    static String provideHomePath() {
        return determineBaseDir().getAbsolutePath();
    }

    private static File determineBaseDir() {
        String envHOMEVariable = System.getenv("HOME");
        if (envHOMEVariable != null) {
            File homeDirectory = new File(envHOMEVariable);
            if (homeDirectory.exists()) {
                return homeDirectory;
            }
        }
        return new File(System.getProperty("user.home"));
    }
}
