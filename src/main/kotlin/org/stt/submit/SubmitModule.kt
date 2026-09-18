package org.stt.submit

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import net.engio.mbassy.bus.MBassador
import org.stt.config.ConfigRoot
import org.stt.config.TempoConfig
import org.stt.connector.jira.JiraConnector
import org.stt.submit.csv.CsvItemsSubmitConnector
import org.stt.submit.csv.CsvSummarySubmitConnector
import org.stt.submit.json.JsonSubmitConnector
import org.stt.submit.tempo.TempoApiClient
import org.stt.submit.tempo.TempoSubmitConnector
import org.stt.time.DurationRounder
import java.nio.charset.StandardCharsets
import java.util.Optional
import javax.inject.Named
import javax.inject.Singleton

@Module
class SubmitModule {

    @Provides
    @Singleton
    fun provideSubmitSelectionManager(): SubmitSelectionManager {
        return SubmitSelectionManager()
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideJsonSubmitConnector(configRoot: org.stt.config.ConfigRoot, @Named("homePath") homePath: String,
                                   submitStatusTracker: SubmitStatusTracker): SubmitConnector {
        val connectorConfig = configRoot.submit.connectors.firstOrNull { it.type == "json" }
            ?: ConnectorConfig(type = "json", file = ".stt/submit.json")
        return JsonSubmitConnector(connectorConfig, homePath, submitStatusTracker)
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideCsvItemsSubmitConnector(configRoot: org.stt.config.ConfigRoot, @Named("homePath") homePath: String,
                                       submitStatusTracker: SubmitStatusTracker): SubmitConnector {
        val connectorConfig = configRoot.submit.connectors.firstOrNull { it.type == "csv-items" }
            ?: ConnectorConfig(type = "csv-items", file = ".stt/submit-items.csv")
        return CsvItemsSubmitConnector(connectorConfig, homePath, submitStatusTracker)
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideCsvSummarySubmitConnector(configRoot: org.stt.config.ConfigRoot, @Named("homePath") homePath: String,
                                         submitStatusTracker: SubmitStatusTracker): SubmitConnector {
        val connectorConfig = configRoot.submit.connectors.firstOrNull { it.type == "csv-summary" }
            ?: ConnectorConfig(type = "csv-summary", file = ".stt/submit-summary.csv")
        return CsvSummarySubmitConnector(connectorConfig, homePath, submitStatusTracker)
    }

    @Provides
    @Singleton
    fun provideTempoApiClient(tempoConfig: TempoConfig): TempoApiClient? {
        return tempoConfig.tempoToken?.let {
            TempoApiClient(TempoApiClient.TEMPO_CLOUD_BASE_URL, String(it.password, StandardCharsets.UTF_8))
        }
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideTempoSubmitConnector(tempoConfig: TempoConfig, jiraConnector: JiraConnector,
                                    durationRounder: DurationRounder, submitStatusTracker: SubmitStatusTracker,
                                    eventBus: Optional<MBassador<Any>>, tempoApiClient: TempoApiClient?): SubmitConnector {
        return TempoSubmitConnector(tempoConfig, jiraConnector, durationRounder, submitStatusTracker, eventBus, tempoApiClient)
    }
}