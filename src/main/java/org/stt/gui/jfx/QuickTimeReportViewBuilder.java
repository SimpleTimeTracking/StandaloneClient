package org.stt.gui.jfx;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import org.stt.reporting.QuickTimeReportGenerator;

import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 29.03.15.
 */
public class QuickTimeReportViewBuilder {
    private final ResourceBundle i18n;
    private ObservableValue<QuickTimeReportGenerator.QuickTimeReport> reportValue;

    public QuickTimeReportViewBuilder(ResourceBundle i18n, ObservableValue<QuickTimeReportGenerator.QuickTimeReport> reportValue) {
        this.i18n = checkNotNull(i18n);
        this.reportValue = checkNotNull(reportValue);
    }

    public Pane build() {
        FlowPane flowPane = new FlowPane();
        flowPane.setVgap(8);
        ObservableList<Node> elements = flowPane.getChildren();
        elements.add(new Label(i18n.getString("remainingWorktimeToday")));
        Label remainingWorktimeToday = new Label();
        remainingWorktimeToday.textProperty().bind(new StringBinding() {
            {
                bind(reportValue);
            }
            @Override
            protected String computeValue() {
                return reportValue.getValue().toString();
            }
        });
        return flowPane;
    }
}
