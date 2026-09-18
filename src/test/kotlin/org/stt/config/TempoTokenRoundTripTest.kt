package org.stt.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Tests for the persistence of the `tempo.tempoToken` setting through
 * [YamlConfigService] and [JsonConfigService].
 *
 * Verifies that a token entered via the settings UI round-trips through the config file,
 * that it is never written as plain text, and that it is restored decryptable on reload.
 */
class TempoTokenRoundTripTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private lateinit var homePath: String

    @Before
    fun setup() {
        homePath = temporaryFolder.root.absolutePath
    }

    @Test
    fun shouldStoreTempoTokenEncryptedInYamlAndRoundTrip() {
        // GIVEN
        val writer = YamlConfigService(homePath)
        writer.start()
        writer.config.tempo.tempoToken = PasswordSetting.fromPassword("secretToken".toByteArray(StandardCharsets.UTF_8))

        // WHEN
        writer.stop()

        // THEN
        val yaml = File(temporaryFolder.root, ".stt/stt.yaml").readText(StandardCharsets.UTF_8)
        assertThat(yaml).contains("!encrypted")
        assertThat(yaml).doesNotContain("secretToken")

        // WHEN
        val reader = YamlConfigService(homePath)
        reader.start()

        // THEN
        assertThat(reader.config.tempo.tempoToken!!.password)
                .isEqualTo("secretToken".toByteArray(StandardCharsets.UTF_8))
    }

    @Test
    fun shouldStoreTempoTokenEncodedInJsonAndRoundTrip() {
        // GIVEN
        val writer = JsonConfigService(homePath)
        writer.start()
        writer.config.tempo.tempoToken = PasswordSetting.fromPassword("secretToken".toByteArray(StandardCharsets.UTF_8))

        // WHEN
        writer.stop()

        // THEN
        val json = File(temporaryFolder.root, ".stt/stt.json").readText(StandardCharsets.UTF_8)
        assertThat(json).doesNotContain("secretToken")

        // WHEN
        val reader = JsonConfigService(homePath)
        reader.start()

        // THEN
        assertThat(reader.config.tempo.tempoToken!!.password)
                .isEqualTo("secretToken".toByteArray(StandardCharsets.UTF_8))
    }
}
