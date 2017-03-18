package org.stt.gui.jfx;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.stt.event.TimePassedEvent;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.model.ItemModified;
import org.stt.query.WorkTimeQueries;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * Shows week worktime and day work-/overtime.
 * (First update is done after a second, to prevent blocking opening the UI)
 */
public class WorktimePane extends FlowPane {
    private final ResourceBundle i18n;
    private final WorkTimeQueries workTimeQueries;
    private final SimpleObjectProperty<Duration> remainingWorktime = new SimpleObjectProperty<>(Duration.ZERO);
    private final SimpleObjectProperty<Duration> weekWorktime = new SimpleObjectProperty<>(Duration.ZERO);

    @Inject
    public WorktimePane(ResourceBundle i18n,
                        MBassador<Object> eventbus,
                        WorkTimeQueries workTimeQueries) {
        this.i18n = requireNonNull(i18n);
        this.workTimeQueries = requireNonNull(workTimeQueries);
        requireNonNull(eventbus).subscribe(this);

        build();
    }

    private void build() {
        setHgap(20);
        setPadding(new Insets(2));
        ObservableList<Node> elements = getChildren();
        Label remainingWorktimeToday = new Label();
        ObservableValue<Duration> workOrOvertime = Bindings.createObjectBinding(() -> remainingWorktime.get().abs(), remainingWorktime);
        remainingWorktimeToday.textProperty().bind(STTBindings.formattedDuration(workOrOvertime));
        Label weekWorktimeLabel = new Label();
        weekWorktimeLabel.textProperty().bind(STTBindings.formattedDuration(weekWorktime));

        Label workOrOvertimeLabel = new Label();
        workOrOvertimeLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> i18n.getString(remainingWorktime.get().isNegative() ? "overtimeToday" : "remainingWorktimeToday"),
                        remainingWorktime)
        );

        elements.add(hbox(4, workOrOvertimeLabel, remainingWorktimeToday));
        elements.add(hbox(4, new Label(i18n.getString("weekWorktime")), weekWorktimeLabel));
    }

    private HBox hbox(double spacing, Node... nodes) {
        HBox hBox = new HBox(spacing);
        hBox.getChildren().addAll(nodes);
        return hBox;
    }

    @Handler
    public void timePassed(TimePassedEvent event) {
        updateWorktime();
    }

    @Handler
    public void onItemChange(ItemModified event) {
        updateWorktime();
    }

    private void updateWorktime() {
        remainingWorktime.setValue(workTimeQueries.queryRemainingWorktimeToday());
        weekWorktime.setValue(workTimeQueries.queryWeekWorktime());
    }

}
