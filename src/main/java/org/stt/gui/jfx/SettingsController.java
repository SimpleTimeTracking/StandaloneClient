package org.stt.gui.jfx;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
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

    private PropertyEditor<?> createPasswordSettingsEditor(PropertySheet.Item property) {
        AbstractPropertyEditor<PasswordSetting, PasswordField> propertyEditor = new AbstractPropertyEditor<PasswordSetting, PasswordField>(property, new PasswordField()) {
            @Override
            protected ObjectBinding<PasswordSetting> getObservableValue() {
                StringProperty textProperty = getEditor().textProperty();
                return Bindings.createObjectBinding(() -> textProperty.getValue() == null ? null : PasswordSetting.fromPassword(textProperty.getValue().getBytes(StandardCharsets.UTF_8)), textProperty);
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
            protected ObjectBinding<PathSetting> getObservableValue() {
                StringProperty textProperty = getEditor().textProperty();
                return Bindings.createObjectBinding(() -> new PathSetting(textProperty.getValue()), textProperty);
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
        return pane;
    }
}
