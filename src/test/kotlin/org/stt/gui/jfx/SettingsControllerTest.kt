package org.stt.gui.jfx

import javafx.scene.control.PasswordField
import javafx.scene.layout.BorderPane
import org.assertj.core.api.Assertions.assertThat
import org.controlsfx.control.PropertySheet
import org.controlsfx.property.editor.AbstractPropertyEditor
import org.controlsfx.property.editor.PropertyEditor
import org.junit.Before
import org.junit.Test
import org.stt.config.ActivitiesConfig
import org.stt.config.BackupConfig
import org.stt.config.CliConfig
import org.stt.config.CommonPrefixGrouperConfig
import org.stt.config.ConfigRoot
import org.stt.config.JiraConfig
import org.stt.config.PasswordSetting
import org.stt.config.ReportConfig
import org.stt.config.TempoConfig
import org.stt.config.WorktimeConfig
import java.nio.charset.StandardCharsets

/**
 * Tests for [SettingsController].
 *
 * Verifies that the property sheet lists the properties of all configuration containers,
 * including the Tempo configuration, and that secret settings such as the Tempo token are
 * edited with a masked editor and re-encrypted on edit.
 */
class SettingsControllerTest {

    private lateinit var sut: SettingsController

    @Before
    fun setup() {
        TestFX.installTK()
        sut = SettingsController(ConfigRoot(), ActivitiesConfig(), BackupConfig(), CliConfig(),
                CommonPrefixGrouperConfig(), ReportConfig(), WorktimeConfig(), JiraConfig(),
                TempoConfig())
    }

    @Test
    fun shouldListTempoPropertiesInPropertySheet() {
        // GIVEN
        val propertySheet = propertySheet()

        // WHEN
        val itemNames = propertySheet.items.map { it.name }

        // THEN
        assertThat(itemNames).contains("tempoToken", "workdayStart", "summaryMode")
    }

    @Test
    fun shouldEditTempoTokenWithMaskedEditor() {
        // GIVEN
        val propertySheet = propertySheet()
        val tempoTokenItem = tempoTokenItem(propertySheet)

        // WHEN
        val editor = propertySheet.propertyEditorFactory.call(tempoTokenItem)

        // THEN
        assertThat(editor.editor).isInstanceOf(PasswordField::class.java)
    }

    @Test
    fun shouldEncryptEditedTempoToken() {
        // GIVEN
        val propertySheet = propertySheet()
        val tempoTokenItem = tempoTokenItem(propertySheet)
        val editor = propertySheet.propertyEditorFactory.call(tempoTokenItem)
                as AbstractPropertyEditor<PasswordSetting, PasswordField>
        val plainToken = "topsecret"

        // WHEN
        editor.editor.text = plainToken

        // THEN
        val storedValue = editor.value
        assertThat(storedValue.password).isEqualTo(plainToken.toByteArray(StandardCharsets.UTF_8))
        assertThat(storedValue.encodedPassword).isNotEqualTo(plainToken.toByteArray(StandardCharsets.UTF_8))
    }

    private fun propertySheet(): PropertySheet {
        return (sut.panel as BorderPane).center as PropertySheet
    }

    private fun tempoTokenItem(propertySheet: PropertySheet): PropertySheet.Item {
        val items = propertySheet.items.filter { it.name == "tempoToken" }
        assertThat(items).hasSize(1)
        return items[0]
    }
}
