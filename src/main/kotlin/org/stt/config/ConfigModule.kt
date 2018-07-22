package org.stt.config


import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Named

@Module
class ConfigModule {

    @Provides
    fun provideConfigRoot(configService: ConfigServiceFacade): ConfigRoot {
        return configService.config
    }

    @Provides
    fun provideTimeTrackingItemListConfig(configRoot: ConfigRoot): ActivitiesConfig {
        return configRoot.activities
    }

    @Provides
    fun provideWorktimeConfig(configRoot: ConfigRoot): WorktimeConfig {
        return configRoot.worktime
    }

    @Provides
    fun provideReportConfig(configRoot: ConfigRoot): ReportConfig {
        return configRoot.report
    }

    @Provides
    fun provideCliConfig(configRoot: ConfigRoot): CliConfig {
        return configRoot.cli
    }

    @Provides
    fun provideBackupConfig(configRoot: ConfigRoot): BackupConfig {
        return configRoot.backup
    }

    @Provides
    fun provideCommonPrefixGrouperConfig(configRoot: ConfigRoot): CommonPrefixGrouperConfig {
        return configRoot.prefixGrouper
    }

    @Provides
    fun provideJiraConfig(configRoot: ConfigRoot): JiraConfig {
        return configRoot.jira
    }

    @Provides
    @Named("homePath")
    fun provideHomePath(): String {
        return determineBaseDir().absolutePath
    }

    private fun determineBaseDir(): File {
        val envHOMEVariable = System.getenv("HOME")
        if (envHOMEVariable != null) {
            val homeDirectory = File(envHOMEVariable)
            if (homeDirectory.exists()) {
                return homeDirectory
            }
        }
        return File(System.getProperty("user.home"))
    }
}
