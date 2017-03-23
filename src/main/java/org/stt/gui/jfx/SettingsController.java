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
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;

public class SettingsController {
    private Pane pane;

    @Inject
    public SettingsController(ConfigRoot configRoot,
                              ActivitiesConfig activitiesConfig,
                              BackupConfig backupConfig,
                              CliConfig cliConfig,
                              CommonPrefixGrouperConfig commonPrefixGrouperConfig,
                              ReportConfig reportConfig,
                              WorktimeConfig worktimeConfig) {
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
                reportConfig, worktimeConfig)
                .forEach(o -> propertySheet.getItems().addAll(BeanPropertyUtils.getProperties(o,
                        propertyDescriptor -> {
                            Class<?> propertyType = propertyDescriptor.getPropertyType();
                            return !(ConfigurationContainer.class.isAssignableFrom(propertyType));
                        })));
        pane = new BorderPane(propertySheet);
    }

    private PropertyEditor<?> createPasswordSettingsEditor(PropertySheet.Item property) {
        return new AbstractPropertyEditor<PasswordSetting, PasswordField>(property, new PasswordField()) {

            {
                enableAutoSelectAll(getEditor());
            }

            @Override
            protected ObjectBinding<PasswordSetting> getObservableValue() {
                StringProperty textProperty = getEditor().textProperty();
                return Bindings.createObjectBinding(() -> textProperty.getValue() == null ? null : PasswordSetting.fromPassword(textProperty.getValue().getBytes("UTF-8")), textProperty);
            }

            @Override
            public void setValue(PasswordSetting value) {
                try {
                    getEditor().setText(value == null ? null : new String(value.getPassword(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    private static AbstractPropertyEditor<PathSetting, TextField> createPathSettingsEditor(PropertySheet.Item property) {
        return new AbstractPropertyEditor<PathSetting, TextField>(property, new TextField()) {

            {
                enableAutoSelectAll(getEditor());
            }

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
