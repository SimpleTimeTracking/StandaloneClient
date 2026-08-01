package org.stt.submit

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
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
}