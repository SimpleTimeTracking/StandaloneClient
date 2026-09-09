package org.stt.submit

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import org.stt.submit.csv.CsvItemsSubmitConnector
import org.stt.submit.csv.CsvSummarySubmitConnector
import org.stt.submit.json.JsonSubmitConnector
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
    @IntoSet
    fun provideJsonSubmitConnector(configRoot: org.stt.config.ConfigRoot, @Named("homePath") homePath: String): SubmitConnector {
        val connectorConfig = configRoot.submit.connectors.firstOrNull { it.type == "json" }
            ?: ConnectorConfig(type = "json", file = ".stt/submit.json")
        return JsonSubmitConnector(connectorConfig, homePath)
    }

    @Provides
    @IntoSet
    fun provideCsvItemsSubmitConnector(configRoot: org.stt.config.ConfigRoot, @Named("homePath") homePath: String): SubmitConnector {
        val connectorConfig = configRoot.submit.connectors.firstOrNull { it.type == "csv-items" }
            ?: ConnectorConfig(type = "csv-items", file = ".stt/submit-items.csv")
        return CsvItemsSubmitConnector(connectorConfig, homePath)
    }

    @Provides
    @IntoSet
    fun provideCsvSummarySubmitConnector(configRoot: org.stt.config.ConfigRoot, @Named("homePath") homePath: String): SubmitConnector {
        val connectorConfig = configRoot.submit.connectors.firstOrNull { it.type == "csv-summary" }
            ?: ConnectorConfig(type = "csv-summary", file = ".stt/submit-summary.csv")
        return CsvSummarySubmitConnector(connectorConfig, homePath)
    }
}