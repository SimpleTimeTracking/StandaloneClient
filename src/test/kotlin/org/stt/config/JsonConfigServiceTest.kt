package org.stt.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.stt.submit.ConnectorConfig
import org.stt.submit.SubmitConfig

/**
 * Verifies that the JSON configuration service persists and reloads the
 * submit configuration, including the last-used connector id, across
 * service restarts.
 */
class JsonConfigServiceTest {

    @get:Rule
    var tempFolder = TemporaryFolder()

    private lateinit var homePath: String

    @Before
    fun setup() {
        homePath = tempFolder.root.absolutePath
    }

    @Test
    fun shouldRoundTripLastUsedConnectorId() {
        // GIVEN
        val writingService = JsonConfigService(homePath)
        writingService.start()
        writingService.config.submit = SubmitConfig(
                connectors = listOf(ConnectorConfig(type = "json", file = ".stt/submit.json")),
                lastUsedConnectorId = "tempo")
        writingService.stop()

        // WHEN
        val readingService = JsonConfigService(homePath)
        readingService.start()

        // THEN
        assertThat(readingService.config.submit.lastUsedConnectorId).isEqualTo("tempo")
        assertThat(readingService.config.submit.connectors).hasSize(1)
        assertThat(readingService.config.submit.connectors[0].type).isEqualTo("json")
    }

    @Test
    fun shouldDefaultToNullLastUsedConnectorIdWhenMissingInConfig() {
        // GIVEN - config file without the new field
        val legacyService = JsonConfigService(homePath)
        legacyService.start()
        legacyService.stop()

        // WHEN
        val readingService = JsonConfigService(homePath)
        readingService.start()

        // THEN
        assertThat(readingService.config.submit.lastUsedConnectorId).isNull()
    }
}
