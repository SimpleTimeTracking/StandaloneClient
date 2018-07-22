package org.stt.gui.jfx

import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import org.controlsfx.control.PropertySheet
import org.controlsfx.property.BeanPropertyUtils
import org.controlsfx.property.editor.AbstractPropertyEditor
import org.controlsfx.property.editor.PropertyEditor
import org.stt.config.*
import java.nio.charset.StandardCharsets
import java.util.stream.Stream
import javax.inject.Inject

class SettingsController @Inject
constructor(private val configRoot: ConfigRoot, // NOSONAR
            private val activitiesConfig: ActivitiesConfig,
            private val backupConfig: BackupConfig,
            private val cliConfig: CliConfig,
            private val commonPrefixGrouperConfig: CommonPrefixGrouperConfig,
            private val reportConfig: ReportConfig,
            private val worktimeConfig: WorktimeConfig,
            private val jiraConfig: JiraConfig) {
    private var pane: Pane? = null

    val panel: Node?
        get() {
            initialize()
            return pane
        }

    private fun createPasswordSettingsEditor(property: PropertySheet.Item): PropertyEditor<*> {
        val propertyEditor = object : AbstractPropertyEditor<PasswordSetting, PasswordField>(property, PasswordField()) {
            override fun getObservableValue(): ObservableValue<PasswordSetting> {
                val textProperty = editor.textProperty()
                val passwordSettingProperty = SimpleObjectProperty<PasswordSetting>()
                applyValue(passwordSettingProperty, textProperty.value)
                textProperty.addListener { _, _, newValue -> applyValue(passwordSettingProperty, newValue) }
                return passwordSettingProperty
            }

            private fun applyValue(passwordSettingProperty: ObjectProperty<PasswordSetting>, newValue: String?) {
                passwordSettingProperty.value = if (newValue == null) null else PasswordSetting.fromPassword(newValue.toByteArray(StandardCharsets.UTF_8))
            }

            override fun setValue(value: PasswordSetting?) {
                editor.text = if (value == null) null else String(value.password, StandardCharsets.UTF_8)
            }
        }
        enableAutoSelectAll(propertyEditor.editor)
        return propertyEditor
    }

    private fun createPathSettingsEditor(property: PropertySheet.Item): AbstractPropertyEditor<PathSetting, TextField> {
        val propertyEditor = object : AbstractPropertyEditor<PathSetting, TextField>(property, TextField()) {
            override fun getObservableValue(): ObservableValue<PathSetting> {
                val textProperty = editor.textProperty()
                val pathSettingProperty = SimpleObjectProperty<PathSetting>()
                applyValue(pathSettingProperty, textProperty.value)
                textProperty.addListener { _, _, newValue -> applyValue(pathSettingProperty, newValue) }
                return pathSettingProperty
            }

            private fun applyValue(pathSettingProperty: ObjectProperty<PathSetting>, newValue: String) {
                pathSettingProperty.value = PathSetting(newValue)
            }

            override fun setValue(value: PathSetting) {
                editor.text = value.path()
            }
        }
        enableAutoSelectAll(propertyEditor.editor)
        return propertyEditor
    }

    private fun enableAutoSelectAll(control: TextInputControl) {
        control.focusedProperty().addListener { _: ObservableValue<out Boolean>, _: Boolean, newValue: Boolean ->
            if (newValue) {
                Platform.runLater { control.selectAll() }
            }
        }
    }

    private fun initialize() {
        val propertySheet = PropertySheet()
        val defaultEditorFactory = propertySheet.propertyEditorFactory
        propertySheet.setPropertyEditorFactory { item ->
            val type = item.type
            if (PathSetting::class.java.isAssignableFrom(type)) {
                return@setPropertyEditorFactory createPathSettingsEditor(item)
            }
            if (PasswordSetting::class.java.isAssignableFrom(type)) {
                return@setPropertyEditorFactory createPasswordSettingsEditor(item)
            }
            defaultEditorFactory.call(item)
        }
        Stream.of(configRoot, activitiesConfig, backupConfig, cliConfig, commonPrefixGrouperConfig,
                reportConfig, worktimeConfig, jiraConfig)
                .forEach { o ->
                    propertySheet.items.addAll(BeanPropertyUtils.getProperties(o
                    ) { propertyDescriptor ->
                        val propertyType = propertyDescriptor.propertyType
                        !ConfigurationContainer::class.java.isAssignableFrom(propertyType)
                    })
                }
        pane = BorderPane(propertySheet)
    }
}
