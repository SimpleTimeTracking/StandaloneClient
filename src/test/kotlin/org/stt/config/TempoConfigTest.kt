package org.stt.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Duration

/**
 * Tests for [TempoConfig] YAML persistence.
 *
 * Verifies that the `tempo:` section with an encrypted token, the workday start duration and the
 * summary mode survive a YAML write/load round trip, and that defaults are kept when the section
 * is absent from the file.
 */
class TempoConfigTest {

    @Test
    fun shouldRoundTripTempoConfigThroughYaml() {
        // GIVEN
        val homePath = Files.createTempDirectory("stt-home").toFile().absolutePath
        val writer = YamlConfigService(homePath)
        writer.start()
        writer.config.tempo = TempoConfig().apply {
            tempoToken = PasswordSetting.fromPassword("tempo-secret".toByteArray(StandardCharsets.UTF_8))
            workdayStart = Duration.ofHours(9)
            summaryMode = TempoSummaryMode.GROUPED
        }

        // WHEN
        writer.stop()
        val reader = YamlConfigService(homePath)
        reader.start()
        val loaded = reader.config.tempo

        // THEN
        assertThat(String(loaded.tempoToken!!.password, StandardCharsets.UTF_8)).isEqualTo("tempo-secret")
        assertThat(loaded.workdayStart).isEqualTo(Duration.ofHours(9))
        assertThat(loaded.summaryMode).isEqualTo(TempoSummaryMode.GROUPED)
    }

    @Test
    fun shouldKeepDefaultsWhenTempoSectionIsAbsent() {
        // GIVEN
        val homePath = Files.createTempDirectory("stt-home").toFile().absolutePath
        val writer = YamlConfigService(homePath)
        writer.start()
        writer.config.tempo = TempoConfig()
        writer.stop()
        val yamlFile = Files.readAllLines(java.io.File("$homePath/.stt/stt.yaml").toPath())
        val withoutTempoSection = yamlFile.filterNot { it.contains("tempo:") }

        // WHEN
        Files.write(java.io.File("$homePath/.stt/stt.yaml").toPath(), withoutTempoSection)
        val reader = YamlConfigService(homePath)
        reader.start()
        val loaded = reader.config.tempo

        // THEN
        assertThat(loaded.tempoToken).isNull()
        assertThat(loaded.workdayStart).isEqualTo(Duration.ofHours(8))
        assertThat(loaded.summaryMode).isEqualTo(TempoSummaryMode.ITEMS)
    }
}
