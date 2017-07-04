package org.stt.gui.jfx;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanPropertyUtils;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.PropertyEditor;
import org.stt.config.*;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class SettingsController {
    private final ConfigRoot configRoot;
    private final ActivitiesConfig activitiesConfig;
    private final BackupConfig backupConfig;
    private final CliConfig cliConfig;
    private final CommonPrefixGrouperConfig commonPrefixGrouperConfig;
    private final ReportConfig reportConfig;
    private final WorktimeConfig worktimeConfig;
    private final JiraConfig jiraConfig;
    private Pane pane;

    @Inject
    public SettingsController(ConfigRoot configRoot, // NOSONAR
                              ActivitiesConfig activitiesConfig,
                              BackupConfig backupConfig,
                              CliConfig cliConfig,
                              CommonPrefixGrouperConfig commonPrefixGrouperConfig,
                              ReportConfig reportConfig,
                              WorktimeConfig worktimeConfig,
                              JiraConfig jiraConfig) {
        this.configRoot = configRoot;
        this.activitiesConfig = activitiesConfig;
        this.backupConfig = backupConfig;
        this.cliConfig = cliConfig;
        this.commonPrefixGrouperConfig = commonPrefixGrouperConfig;
        this.reportConfig = reportConfig;
        this.worktimeConfig = worktimeConfig;
        this.jiraConfig = jiraConfig;
    }

    private PropertyEditor<?> createPasswordSettingsEditor(PropertySheet.Item property) {
        AbstractPropertyEditor<PasswordSetting, PasswordField> propertyEditor = new AbstractPropertyEditor<PasswordSetting, PasswordField>(property, new PasswordField()) {
            @Override
            protected ObservableValue<PasswordSetting> getObservableValue() {
                StringProperty textProperty = getEditor().textProperty();
                ObjectProperty<PasswordSetting> passwordSettingProperty = new SimpleObjectProperty<>();
                applyValue(passwordSettingProperty, textProperty.getValue());
                textProperty.addListener((observable, oldValue, newValue) -> applyValue(passwordSettingProperty, newValue));
                return passwordSettingProperty;
            }

            private void applyValue(ObjectProperty<PasswordSetting> passwordSettingProperty, String newValue) {
                passwordSettingProperty.setValue(newValue == null ? null : PasswordSetting.fromPassword(newValue.getBytes(StandardCharsets.UTF_8)));
            }

            @Override
            public void setValue(PasswordSetting value) {
                getEditor().setText(value == null ? null : new String(value.getPassword(), StandardCharsets.UTF_8));
            }
        };
        enableAutoSelectAll(propertyEditor.getEditor());
        return propertyEditor;
    }

    private static AbstractPropertyEditor<PathSetting, TextField> createPathSettingsEditor(PropertySheet.Item property) {
        AbstractPropertyEditor<PathSetting, TextField> propertyEditor = new AbstractPropertyEditor<PathSetting, TextField>(property, new TextField()) {
            @Override
            protected ObservableValue<PathSetting> getObservableValue() {
                StringProperty textProperty = getEditor().textProperty();
                ObjectProperty<PathSetting> pathSettingProperty = new SimpleObjectProperty<>();
                applyValue(pathSettingProperty, textProperty.getValue());
                textProperty.addListener((observable, oldValue, newValue) -> applyValue(pathSettingProperty, newValue));
                return pathSettingProperty;
            }

            private void applyValue(ObjectProperty<PathSetting> pathSettingProperty, String newValue) {
                pathSettingProperty.setValue(new PathSetting(newValue));
            }

            @Override
            public void setValue(PathSetting value) {
                getEditor().setText(value.path());
            }
        };
        enableAutoSelectAll(propertyEditor.getEditor());
        return propertyEditor;
    }

    private static void enableAutoSelectAll(final TextInputControl control) {
        control.focusedProperty().addListener((ObservableValue<? extends Boolean> o, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                Platform.runLater(control::selectAll);
            }
        });
    }

    public Node getPanel() {
        initialize();
        return pane;
    }

    private void initialize() {
        PropertySheet propertySheet = new PropertySheet();
        Callback<PropertySheet.Item, PropertyEditor<?>> defaultEditorFactory = propertySheet.getPropertyEditorFactory();
        propertySheet.setPropertyEditorFactory(item -> {
            Class<?> type = item.getType();
            if (PathSetting.class.isAssignableFrom(type)) {
                return createPathSettingsEditor(item);
            }
            if (PasswordSetting.class.isAssignableFrom(type)) {
                return createPasswordSettingsEditor(item);
            }
            return defaultEditorFactory.call(item);
        });
        Stream.of(configRoot, activitiesConfig, backupConfig, cliConfig, commonPrefixGrouperConfig,
                reportConfig, worktimeConfig, jiraConfig)
                .forEach(o -> propertySheet.getItems().addAll(BeanPropertyUtils.getProperties(o,
                        propertyDescriptor -> {
                            Class<?> propertyType = propertyDescriptor.getPropertyType();
                            return !(ConfigurationContainer.class.isAssignableFrom(propertyType));
                        })));
        pane = new BorderPane(propertySheet);
    }
}
